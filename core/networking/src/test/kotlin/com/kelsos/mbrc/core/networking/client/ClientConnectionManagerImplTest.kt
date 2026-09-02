package com.kelsos.mbrc.core.networking.client

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import com.kelsos.mbrc.core.common.data.ConnectionSettings
import com.kelsos.mbrc.core.common.state.ConnectionStatePublisher
import com.kelsos.mbrc.core.common.state.ConnectionStatus
import com.kelsos.mbrc.core.common.test.coroutineTestTimeout
import com.kelsos.mbrc.core.common.test.testDispatcher
import com.kelsos.mbrc.core.common.test.testDispatcherModule
import com.kelsos.mbrc.core.networking.ClientConnectionManagerImpl
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
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
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

  private val connectionManager: ClientConnectionManagerImpl by inject()
  private val activityChecker: SocketActivityChecker by inject()
  private val messageHandler: MessageHandler by inject()
  private val pendingCommands: PendingCommandBuffer by inject()
  private val connectionProvider: ConnectionProvider by inject()
  private val connectionState: ConnectionStatePublisher by inject()
  private val uiMessageQueue: UiMessageQueue by inject()

  private val connectionStateFlow = MutableStateFlow<ConnectionStatus>(ConnectionStatus.Offline)
  private val uiMessageFlow = MutableSharedFlow<UiMessage>(extraBufferCapacity = 5)

  @Before
  fun setUp() {
    startKoin {
      modules(listOf(testModule, testDispatcherModule))
    }

    // Setup mock connection state flow
    every { connectionState.connection } returns connectionStateFlow
    coEvery { connectionState.updateConnection(any()) } answers {
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
