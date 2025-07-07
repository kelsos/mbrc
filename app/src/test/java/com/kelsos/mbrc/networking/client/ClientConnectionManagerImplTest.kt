package com.kelsos.mbrc.networking.client

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.kelsos.mbrc.common.state.ConnectionStatePublisher
import com.kelsos.mbrc.common.state.ConnectionStatus
import com.kelsos.mbrc.features.settings.ConnectionRepository
import com.kelsos.mbrc.features.settings.ConnectionSettings
import com.kelsos.mbrc.networking.SocketActivityChecker
import com.kelsos.mbrc.networking.discovery.DiscoveryStop
import com.kelsos.mbrc.utils.testDispatcher
import com.kelsos.mbrc.utils.testDispatcherModule
import com.squareup.moshi.Moshi
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.verify
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.module
import org.koin.test.KoinTest
import org.koin.test.inject

@RunWith(AndroidJUnit4::class)
class ClientConnectionManagerImplTest : KoinTest {
  private val testModule =
    module {
      single<SocketActivityChecker> { mockk(relaxed = true) }
      single<MessageHandler> { mockk(relaxed = true) }
      single<ConnectionRepository> { mockk(relaxed = true) }
      single<ConnectionStatePublisher> { mockk(relaxed = true) }
      single<UiMessageQueue> { mockk(relaxed = true) }
      single { Moshi.Builder().build() }
      singleOf(::ClientConnectionManagerImpl)
    }

  private val connectionManager: ClientConnectionManagerImpl by inject()
  private val activityChecker: SocketActivityChecker by inject()
  private val connectionRepository: ConnectionRepository by inject()
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
      coEvery { connectionRepository.getDefault() } returns null

      // When
      connectionManager.start()
      delay(3000) // Allow for the 2s delay + some processing time

      // Then - No repository call should be made since already connected
      coVerify(exactly = 0) { connectionRepository.getDefault() }
    }
  }

  @Test
  fun startShouldCallDiscoveryWhenNoDefaultConnection() {
    runTest(testDispatcher) {
      // Given
      coEvery { connectionRepository.getDefault() } returns null
      coEvery { connectionRepository.discover() } returns DiscoveryStop.NotFound

      // When
      connectionManager.start()
      delay(3000) // Allow for the 2s delay + some processing time

      // Then
      coVerify { connectionRepository.getDefault() }
      coVerify { connectionRepository.discover() }
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

  @Test
  fun multipleStartCallsShouldBeHandledGracefully() {
    runTest(testDispatcher) {
      // Given
      coEvery { connectionRepository.getDefault() } returns null
      coEvery { connectionRepository.discover() } returns DiscoveryStop.NotFound

      // When
      connectionManager.start()
      connectionManager.start()
      connectionManager.start()
      delay(3000)

      // Then - Should handle multiple starts without issues
      coVerify(atLeast = 1) { connectionRepository.getDefault() }
    }
  }
}
