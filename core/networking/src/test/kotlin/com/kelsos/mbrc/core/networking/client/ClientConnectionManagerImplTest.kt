package com.kelsos.mbrc.core.networking.client

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import com.kelsos.mbrc.core.common.data.ConnectionSettings
import com.kelsos.mbrc.core.common.state.ConnectionStatePublisher
import com.kelsos.mbrc.core.common.state.ConnectionStatus
import com.kelsos.mbrc.core.common.test.coroutineTestTimeout
import com.kelsos.mbrc.core.common.test.testDispatcher
import com.kelsos.mbrc.core.common.test.testDispatcherModule
import com.kelsos.mbrc.core.common.utilities.coroutines.AppCoroutineDispatchers
import com.kelsos.mbrc.core.networking.ClientConnectionManagerImpl
import com.kelsos.mbrc.core.networking.ConnectionCycleInfo
import com.kelsos.mbrc.core.networking.ConnectionProvider
import com.kelsos.mbrc.core.networking.Listener
import com.kelsos.mbrc.core.networking.LocalNetworkAccess
import com.kelsos.mbrc.core.networking.MessageHandler
import com.kelsos.mbrc.core.networking.SocketActivityChecker
import com.kelsos.mbrc.core.networking.discovery.DiscoveryStop
import com.kelsos.mbrc.core.networking.protocol.Clock
import com.kelsos.mbrc.core.networking.protocol.base.Protocol
import com.squareup.moshi.Moshi
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.slot
import io.mockk.verify
import java.net.ServerSocket
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicInteger
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.Timeout
import org.junit.runner.RunWith
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.module
import org.koin.test.KoinTest
import org.koin.test.inject

@RunWith(AndroidJUnit4::class)
class ClientConnectionManagerImplTest : KoinTest {
  @get:Rule
  val timeout: Timeout = coroutineTestTimeout()

  private val testModule =
    module {
      single<SocketActivityChecker> { mockk(relaxed = true) }
      single<MessageHandler> { mockk(relaxed = true) }
      single<ConnectionProvider> { mockk(relaxed = true) }
      single<ConnectionStatePublisher> { mockk(relaxed = true) }
      single<UiMessageQueue> { mockk(relaxed = true) }
      single { PendingCommandBuffer(Clock { System.currentTimeMillis() }) }
      single<LocalNetworkAccess> { LocalNetworkAccess { localNetworkPermitted } }
      single { Moshi.Builder().build() }
      singleOf(::ClientConnectionManagerImpl)
    }

  private var localNetworkPermitted = true
  private var attemptThread: String? = null

  private val connectionManager: ClientConnectionManagerImpl by inject()
  private val activityChecker: SocketActivityChecker by inject()
  private val messageHandler: MessageHandler by inject()
  private val pendingCommands: PendingCommandBuffer by inject()
  private val connectionProvider: ConnectionProvider by inject()
  private val connectionState: ConnectionStatePublisher by inject()
  private val uiMessageQueue: UiMessageQueue by inject()

  private val connectionStateFlow = MutableStateFlow<ConnectionStatus>(ConnectionStatus.Offline)
  private val publishedStates = mutableListOf<ConnectionStatus>()
  private val uiMessageFlow = MutableSharedFlow<UiMessage>(replay = 10, extraBufferCapacity = 5)

  @Before
  fun setUp() {
    startKoin {
      modules(listOf(testModule, testDispatcherModule))
    }

    publishedStates.clear()

    // Setup mock connection state flow
    every { connectionState.connection } returns connectionStateFlow
    coEvery { connectionState.updateConnection(any()) } answers {
      publishedStates += firstArg<ConnectionStatus>()
      connectionStateFlow.value = firstArg()
    }

    // Setup mock UI message flow
    every { uiMessageQueue.messages } returns uiMessageFlow
  }

  @After
  fun tearDown() {
    connectionManager.stop()
    stopKoin()
  }

  private fun createConnectionSettings(port: Int): ConnectionSettings = ConnectionSettings(
    address = "127.0.0.1",
    port = port,
    name = "Test Connection",
    isDefault = true,
    id = 1
  )

  @Test
  fun startShouldNotAttemptConnectionWhenAlreadyConnected() {
    runTest(testDispatcher) {
      // Given
      connectionStateFlow.tryEmit(ConnectionStatus.Connected)
      every { connectionProvider.getDefault() } returns null

      // When
      connectionManager.start()
      delay(3000) // Allow for the 2s delay + some processing time

      // Then - No repository call should be made since already connected
      verify(exactly = 0) { connectionProvider.getDefault() }
    }
  }

  @Test
  fun startShouldCallDiscoveryWhenNoDefaultConnection() {
    runTest(testDispatcher) {
      // Given
      every { connectionProvider.getDefault() } returns null
      coEvery { connectionProvider.discover() } returns DiscoveryStop.NotFound

      // When
      connectionManager.start()
      delay(3000) // Allow for the 2s delay + some processing time

      // Then
      verify { connectionProvider.getDefault() }
      coVerify { connectionProvider.discover() }
    }
  }

  @Test
  fun stopShouldCleanupActivityChecker() {
    runTest(testDispatcher) {
      // Given
      every { activityChecker.stop() } just runs

      // When
      connectionManager.stop()

      // Then
      verify { activityChecker.stop() }
    }
  }

  /**
   * Regression for #332. The outgoing queue must have exactly one collector, whatever happens to
   * the connections. Collectors used to be launched per connection while the ping timeout path
   * reconnects without going through stop(), so every reconnect left another collector subscribed
   * to the shared queue and each command was also handed to a dead connection, which logged it as
   * skipped.
   */
  @Test
  fun reconnectingAfterAPingTimeoutShouldLeaveExactlyOneOutgoingCollector() {
    runTest(testDispatcher) {
      // Given a server that accepts and immediately drops every connection. The listener then
      // returns straight away instead of blocking the single test dispatcher thread on a read.
      val server = ServerSocket(0)
      val executor = Executors.newCachedThreadPool()
      try {
        executor.execute {
          while (!server.isClosed) {
            runCatching { server.accept().close() }
          }
        }

        every { connectionProvider.getDefault() } returns
          createConnectionSettings(server.localPort)

        val activeCollectors = AtomicInteger(0)
        coEvery { messageHandler.processOutgoing(any()) } coAnswers {
          activeCollectors.incrementAndGet()
          try {
            awaitCancellation()
          } finally {
            activeCollectors.decrementAndGet()
          }
        }

        val pingTimeout = slot<Listener>()
        every { activityChecker.setPingTimeoutListener(capture(pingTimeout)) } just runs

        connectionManager.start()
        delay(3000)
        assertThat(activeCollectors.get()).isEqualTo(1)

        // When the ping timeout forces a reconnect
        pingTimeout.captured.invoke()
        delay(5000)

        // Then the single collector is still the only one
        assertThat(activeCollectors.get()).isEqualTo(1)
      } finally {
        server.close()
        executor.shutdownNow()
      }
    }
  }

  /**
   * The heart of #332: the outgoing queue has no replay buffer, so a command emitted while nothing
   * is collecting is gone for good. Tapping play/pause during a reconnect has to end up in the
   * pending buffer rather than vanishing.
   */
  @Test
  fun aCommandQueuedWhileThereIsNoConnectionShouldBeBufferedForReplay() {
    runTest(testDispatcher) {
      // Given the real outgoing queue, drained by the manager, and no connection
      val queue = MessageQueueImpl()
      coEvery { messageHandler.processOutgoing(any()) } coAnswers {
        val listener = firstArg<(SocketMessage) -> Unit>()
        queue.messages.collect { listener(it) }
      }

      connectionManager.start()
      delay(100)

      // When the user taps play/pause
      queue.queue(SocketMessage.create(Protocol.PlayerPlayPause))
      delay(100)

      // Then the command is held for replay instead of being dropped
      assertThat(pendingCommands.drain())
        .containsExactly(SocketMessage.create(Protocol.PlayerPlayPause))
    }
  }

  @Test
  fun multipleStartCallsShouldBeHandledGracefully() {
    runTest(testDispatcher) {
      // Given
      every { connectionProvider.getDefault() } returns null
      coEvery { connectionProvider.discover() } returns DiscoveryStop.NotFound

      // When
      connectionManager.start()
      connectionManager.start()
      connectionManager.start()
      delay(3000)

      // Then - Should handle multiple starts without issues
      verify(atLeast = 1) { connectionProvider.getDefault() }
    }
  }

  /**
   * The blocking socket connect must not run on the caller's dispatcher. Both production callers
   * suspend on the main dispatcher, where a blocking connect throws NetworkOnMainThreadException
   * on a device and runCatching would swallow it into a permanently failing attempt.
   *
   * Built by hand rather than through Koin because the shared test dispatchers are one instance
   * for all four roles, which cannot tell the io dispatcher apart from the caller's. The thread
   * name is asserted as a prefix because the coroutine debug agent appends a "@coroutine#n" suffix.
   */
  @Test
  fun `connect performs socket work on the io dispatcher`() {
    val ioThreads = Executors.newSingleThreadExecutor { runnable ->
      Thread(runnable, IO_THREAD_NAME)
    }
    try {
      val dispatchers = object : AppCoroutineDispatchers {
        override val main: CoroutineDispatcher = testDispatcher
        override val io: CoroutineDispatcher = ioThreads.asCoroutineDispatcher()
        override val database: CoroutineDispatcher = testDispatcher
        override val network: CoroutineDispatcher = testDispatcher
      }
      val manager = ClientConnectionManagerImpl(
        activityChecker,
        messageHandler,
        Moshi.Builder().build(),
        connectionProvider,
        connectionState,
        dispatchers,
        uiMessageQueue,
        pendingCommands,
        LocalNetworkAccess { true }
      )

      val unusedPort = ServerSocket(0).use { it.localPort }
      every { connectionProvider.getDefault() } answers {
        attemptThread = Thread.currentThread().name
        createConnectionSettings(unusedPort)
      }

      runBlocking { manager.connect() }

      assertThat(attemptThread).startsWith(IO_THREAD_NAME)
      manager.stop()
    } finally {
      ioThreads.shutdownNow()
    }
  }

  /**
   * The service is started asynchronously and calls start() itself, so it can land on top of an
   * attempt the drawer or the retry driver already has in flight. Tearing that down would leave
   * the caller awaiting a handshake for a connection that no longer exists.
   */
  @Test
  fun `start yields to an attempt that is already in flight`() {
    runTest(testDispatcher) {
      connectionStateFlow.value = ConnectionStatus.Connecting(cycle = 1, maxCycles = 5)
      every { connectionProvider.getDefault() } returns null

      connectionManager.start()
      testDispatcher.scheduler.advanceUntilIdle()

      verify(exactly = 0) { connectionProvider.getDefault() }
    }
  }

  @Test
  fun `start yields while a handshake is in progress`() {
    runTest(testDispatcher) {
      connectionStateFlow.value = ConnectionStatus.Authenticating
      every { connectionProvider.getDefault() } returns null

      connectionManager.start()
      testDispatcher.scheduler.advanceUntilIdle()

      verify(exactly = 0) { connectionProvider.getDefault() }
    }
  }

  @Test
  fun `a failed connect makes exactly one attempt`() {
    runTest(testDispatcher) {
      val unusedPort = ServerSocket(0).use { it.localPort }
      every { connectionProvider.getDefault() } returns createConnectionSettings(unusedPort)

      val connected = connectionManager.connect()

      assertThat(connected).isFalse()
      verify(exactly = 1) { connectionProvider.getDefault() }
      verify { connectionState.updateConnection(ConnectionStatus.Offline) }
    }
  }

  /**
   * The handshake is the one round trip with no timeout of its own, so a server that accepts the
   * socket and then says nothing used to leave the app in Authenticating forever.
   *
   * Reaching Authenticating is asserted alongside the failure, because it is what proves the
   * attempt died on the silent handshake rather than on the connect.
   *
   * This is the one test that has to run on wall-clock time. The handshake deadline is a
   * `withTimeoutOrNull`, and on [testDispatcher] its clock is the same single thread that the
   * blocking socket read parks: virtual time cannot advance, so the deadline never fires and the
   * attempt escapes only when the socket's own [Connection.SO_TIMEOUT] expires 30s later. A real
   * pool lets the deadline fire on schedule, at the cost of the handshake timeout in real seconds.
   */
  @Test
  fun `a socket that never completes the handshake is reported as a failed attempt`() {
    val server = ServerSocket(0)
    val executor = Executors.newCachedThreadPool()
    val ioThreads = Executors.newCachedThreadPool { runnable ->
      Thread(runnable, IO_THREAD_NAME)
    }
    try {
      val accepted = mutableListOf<java.net.Socket>()
      executor.execute {
        while (!server.isClosed) {
          runCatching { accepted += server.accept() }
        }
      }
      every { connectionProvider.getDefault() } returns
        createConnectionSettings(server.localPort)
      coEvery { messageHandler.processOutgoing(any()) } coAnswers { awaitCancellation() }

      val manager = ClientConnectionManagerImpl(
        activityChecker,
        messageHandler,
        Moshi.Builder().build(),
        connectionProvider,
        connectionState,
        realIoDispatchers(ioThreads.asCoroutineDispatcher()),
        uiMessageQueue,
        pendingCommands,
        LocalNetworkAccess { true }
      )

      val connected = runBlocking { manager.connect() }

      assertThat(connected).isFalse()
      verify { connectionState.updateConnection(ConnectionStatus.Authenticating) }
      verify { connectionState.updateConnection(ConnectionStatus.Offline) }
      verify { activityChecker.stop() }
      manager.stop()
    } finally {
      server.close()
      executor.shutdownNow()
      ioThreads.shutdownNow()
    }
  }

  private fun realIoDispatchers(io: CoroutineDispatcher): AppCoroutineDispatchers =
    object : AppCoroutineDispatchers {
      override val main: CoroutineDispatcher = testDispatcher
      override val io: CoroutineDispatcher = io
      override val database: CoroutineDispatcher = testDispatcher
      override val network: CoroutineDispatcher = testDispatcher
    }

  companion object {
    private const val IO_THREAD_NAME = "mbrc-test-io"
  }

  /**
   * Offline is what the state manager reads as a connection loss, so publishing it on the way into
   * an attempt makes a restart look like a fresh drop and starts a second retry loop alongside the
   * attempt that is already running.
   */
  @Test
  fun `starting an attempt does not announce offline first`() {
    runTest(testDispatcher) {
      val unusedPort = ServerSocket(0).use { it.localPort }
      every { connectionProvider.getDefault() } returns createConnectionSettings(unusedPort)

      connectionManager.connect()

      assertThat(publishedStates.first()).isInstanceOf(ConnectionStatus.Connecting::class.java)
    }
  }

  /**
   * The retry driver reports the whole sequence once when it gives up, so an attempt it made must
   * stay quiet. Otherwise a single outage queues one long snackbar per cycle.
   */
  @Test
  fun `a failed attempt inside the retry loop tells the user nothing`() {
    runTest(testDispatcher) {
      val unusedPort = ServerSocket(0).use { it.localPort }
      every { connectionProvider.getDefault() } returns createConnectionSettings(unusedPort)

      connectionManager.connect(ConnectionCycleInfo(cycle = 2, maxCycles = 5))

      assertThat(uiMessageFlow.replayCache).isEmpty()
    }
  }

  @Test
  fun `a failed attempt the user asked for tells them why`() {
    runTest(testDispatcher) {
      val unusedPort = ServerSocket(0).use { it.localPort }
      every { connectionProvider.getDefault() } returns createConnectionSettings(unusedPort)

      connectionManager.connect()

      assertThat(uiMessageFlow.replayCache).isNotEmpty()
    }
  }

  @Test
  fun startShouldRefuseToConnectWhenLocalNetworkAccessIsDenied() {
    runTest(testDispatcher) {
      // Without the permission a connection cannot succeed, so attempting one would show the user
      // a reconnection that blames the network for something only they can fix.
      localNetworkPermitted = false

      connectionManager.start()
      testDispatcher.scheduler.advanceUntilIdle()

      verify { connectionState.updateConnection(ConnectionStatus.LocalNetworkDenied) }
      coVerify(exactly = 0) { connectionProvider.getDefault() }
    }
  }
}
