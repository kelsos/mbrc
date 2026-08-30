package com.kelsos.mbrc.service

import android.app.Application
import android.content.Intent
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import com.kelsos.mbrc.core.common.state.ConnectionStatePublisher
import com.kelsos.mbrc.core.common.state.ConnectionStatus
import com.kelsos.mbrc.core.common.test.coroutineTestTimeout
import com.kelsos.mbrc.core.common.test.testDispatcher
import com.kelsos.mbrc.core.common.test.testDispatcherModule
import com.kelsos.mbrc.core.networking.ClientConnectionUseCase
import com.kelsos.mbrc.core.networking.ConnectionCycleInfo
import com.kelsos.mbrc.core.networking.LocalNetworkAccess
import com.kelsos.mbrc.core.networking.client.UiMessage
import com.kelsos.mbrc.core.networking.client.UiMessageQueue
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.Timeout
import org.junit.runner.RunWith
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.dsl.module
import org.koin.test.KoinTest
import org.koin.test.inject

@RunWith(AndroidJUnit4::class)
class ServiceLifecycleManagerImplTest : KoinTest {
  @get:Rule
  val timeout: Timeout = coroutineTestTimeout()

  private val application: Application = mockk(relaxed = true)
  private val connectionUseCase: ClientConnectionUseCase = mockk(relaxed = true)
  private val connectionState: ConnectionStatePublisher = mockk(relaxed = true)
  private val uiMessageQueue: UiMessageQueue = mockk(relaxed = true)
  private val uiMessages: MutableSharedFlow<UiMessage> = mockk(relaxed = true)
  private var localNetworkPermitted = true

  private val testModule = module {
    single { application }
    single { connectionUseCase }
    single { connectionState }
    single<LocalNetworkAccess> { LocalNetworkAccess { localNetworkPermitted } }
    single<UiMessageQueue> { uiMessageQueue }
    single<ServiceLifecycleManager> {
      ServiceLifecycleManagerImpl(get(), get(), get(), get(), get(), get())
    }
  }

  private fun backoffFor(cycle: Int): Long = minOf(
    ServiceLifecycleManagerImpl.BACKOFF_STEP_MS * cycle,
    ServiceLifecycleManagerImpl.MAX_BACKOFF_MS
  )

  private val firstBackoff get() = backoffFor(1)

  private fun totalBackoff(): Long =
    (1..ServiceLifecycleManagerImpl.MAX_RECONNECTION_CYCLES).sumOf { backoffFor(it) }

  private val serviceLifecycleManager: ServiceLifecycleManager by inject()

  @Before
  fun setUp() {
    startKoin {
      modules(listOf(testModule, testDispatcherModule))
    }

    // Reset ServiceState before each test
    ServiceState.setRunning(false)
    ServiceState.setStopping(false)
    localNetworkPermitted = true

    // Setup default mocks
    coEvery { connectionUseCase.connect(any(), any()) } returns false
    every { application.stopService(any()) } returns true
    every { uiMessageQueue.messages } returns uiMessages
  }

  @Test
  fun `does not reconnect when local network access is denied`() = runTest {
    // Retrying cannot recover a missing permission, and a reconnection cycle would blame the
    // network for something only the user can fix.
    localNetworkPermitted = false
    ServiceState.setRunning(true)

    serviceLifecycleManager.onConnectionLost()
    testDispatcher.scheduler.advanceUntilIdle()

    coVerify(exactly = 0) { connectionUseCase.connect(any(), any()) }
    verify { connectionState.updateConnection(ConnectionStatus.LocalNetworkDenied) }
  }

  @Test
  fun `denied local network access does not report a generic offline state`() = runTest {
    // Offline would overwrite the specific reason and put the UI back to "not connected".
    localNetworkPermitted = false
    ServiceState.setRunning(true)

    serviceLifecycleManager.onConnectionLost()
    testDispatcher.scheduler.advanceUntilIdle()

    verify(exactly = 0) { connectionState.updateConnection(ConnectionStatus.Offline) }
  }

  @After
  fun tearDown() {
    // Reset ServiceState after each test
    ServiceState.setRunning(false)
    ServiceState.setStopping(false)
    stopKoin()
  }

  @Test
  fun onConnectionLostShouldIgnoreWhenServiceNotRunning() {
    runTest(testDispatcher) {
      // Given - service is not running (default state)
      assertThat(ServiceState.isRunning).isFalse()

      // When
      serviceLifecycleManager.onConnectionLost()
      testDispatcher.scheduler.advanceUntilIdle()

      // Then - no reconnection attempts should be made
      coVerify(exactly = 0) { connectionUseCase.connect(any(), any()) }
      assertThat(serviceLifecycleManager.isStopPending).isFalse()
    }
  }

  @Test
  fun onConnectionLostShouldIgnoreWhenServiceIsStopping() {
    runTest(testDispatcher) {
      // Given - service is running but already stopping
      ServiceState.setRunning(true)
      ServiceState.setStopping(true)

      // When
      serviceLifecycleManager.onConnectionLost()
      testDispatcher.scheduler.advanceUntilIdle()

      // Then - no reconnection attempts should be made
      coVerify(exactly = 0) { connectionUseCase.connect(any(), any()) }
      assertThat(serviceLifecycleManager.isStopPending).isFalse()
    }
  }

  @Test
  fun onConnectionLostShouldStartReconnectionWhenServiceIsRunning() {
    runTest(testDispatcher) {
      // Given - service is running
      ServiceState.setRunning(true)

      // When
      serviceLifecycleManager.onConnectionLost()

      // Advance past first delay (15s) to trigger first reconnection attempt
      testDispatcher.scheduler.advanceTimeBy(
        firstBackoff + 100
      )

      // Then - at least one reconnection attempt should be made
      coVerify(atLeast = 1) { connectionUseCase.connect(any(), any()) }
    }
  }

  @Test
  fun onConnectionLostShouldBeIdempotent() {
    runTest(testDispatcher) {
      // Given - service is running
      ServiceState.setRunning(true)

      // When - call onConnectionLost multiple times
      serviceLifecycleManager.onConnectionLost()
      serviceLifecycleManager.onConnectionLost()
      serviceLifecycleManager.onConnectionLost()

      // Advance past first delay to trigger reconnection
      testDispatcher.scheduler.advanceTimeBy(
        firstBackoff + 100
      )

      // Then - should only have one reconnection cycle running
      // After one cycle delay, only one connect call should have been made
      coVerify(exactly = 1) { connectionUseCase.connect(any(), any()) }
    }
  }

  @Test
  fun onConnectionRestoredShouldCancelReconnection() {
    runTest(testDispatcher) {
      // Given - service is running and connection lost
      ServiceState.setRunning(true)
      serviceLifecycleManager.onConnectionLost()

      // Advance a bit but not past the first delay
      testDispatcher.scheduler.advanceTimeBy(firstBackoff / 2)

      // When - connection is restored
      serviceLifecycleManager.onConnectionRestored()

      // Advance past the reconnection delay
      testDispatcher.scheduler.advanceTimeBy(
        firstBackoff + 100
      )

      // Then - no reconnection attempts should be made
      coVerify(exactly = 0) { connectionUseCase.connect(any(), any()) }
      assertThat(serviceLifecycleManager.isStopPending).isFalse()
    }
  }

  @Test
  fun onConnectionRestoredShouldResetIsStopPending() {
    runTest(testDispatcher) {
      // Given - service is running
      ServiceState.setRunning(true)
      serviceLifecycleManager.onConnectionLost()

      // Advance through all cycles to trigger stop
      advanceThroughAllReconnectionCycles()
      assertThat(serviceLifecycleManager.isStopPending).isTrue()

      // When - connection is restored (simulating external reconnection)
      serviceLifecycleManager.onConnectionRestored()

      // Then
      assertThat(serviceLifecycleManager.isStopPending).isFalse()
    }
  }

  @Test
  fun shouldStopServiceAfterMaxReconnectionCycles() {
    runTest(testDispatcher) {
      // Given - service is running
      ServiceState.setRunning(true)

      // When
      serviceLifecycleManager.onConnectionLost()

      // Advance through all reconnection cycles
      advanceThroughAllReconnectionCycles()

      // Then - service should be stopped and state should be Offline
      assertThat(serviceLifecycleManager.isStopPending).isTrue()

      verify { connectionState.updateConnection(ConnectionStatus.Offline) }

      val intentSlot = slot<Intent>()
      verify { application.stopService(capture(intentSlot)) }
      assertThat(intentSlot.captured.component?.className)
        .isEqualTo(RemoteService::class.java.name)
    }
  }

  @Test
  fun shouldCallConnectOnEachCycle() {
    runTest(testDispatcher) {
      // Given - service is running
      ServiceState.setRunning(true)

      // When
      serviceLifecycleManager.onConnectionLost()

      // Advance through all reconnection cycles
      advanceThroughAllReconnectionCycles()

      // Then - connect should be called MAX_RECONNECTION_CYCLES times
      coVerify(exactly = ServiceLifecycleManagerImpl.MAX_RECONNECTION_CYCLES) {
        connectionUseCase.connect(any(), any())
      }
    }
  }

  @Test
  fun `reconnection gives up inside the three minute budget`() {
    val attempts = ServiceLifecycleManagerImpl.MAX_RECONNECTION_CYCLES *
      ServiceLifecycleManagerImpl.ATTEMPT_WORST_CASE_MS
    val total = totalBackoff() + attempts

    assertThat(total).isAtMost(ServiceLifecycleManagerImpl.RECONNECTION_BUDGET_MS)
  }

  /**
   * The loop awaits each attempt's real outcome rather than sleeping a fixed guess, so a cycle that
   * connects has to end the sequence instead of letting the next cycle tear the connection down.
   */
  @Test
  fun `a successful attempt ends the reconnection loop`() {
    runTest(testDispatcher) {
      coEvery { connectionUseCase.connect(any(), any()) } returns false andThen true
      ServiceState.setRunning(true)

      serviceLifecycleManager.onConnectionLost()
      advanceThroughAllReconnectionCycles()

      coVerify(exactly = 2) { connectionUseCase.connect(any(), any()) }
      verify(exactly = 0) { application.stopService(any()) }
    }
  }

  /**
   * Only the driver speaks for the whole sequence, and only once: the attempts themselves stay
   * quiet so a single outage does not queue a snackbar per cycle.
   */
  @Test
  fun `giving up tells the user once`() {
    runTest(testDispatcher) {
      ServiceState.setRunning(true)

      serviceLifecycleManager.onConnectionLost()
      advanceThroughAllReconnectionCycles()

      coVerify(exactly = 1) {
        uiMessages.emit(UiMessage.ConnectionError.AllRetriesExhausted)
      }
    }
  }

  @Test
  fun `giving up does not start a second reconnection loop`() {
    runTest(testDispatcher) {
      ServiceState.setRunning(true)
      serviceLifecycleManager.onConnectionLost()
      advanceThroughAllReconnectionCycles()

      val attemptsBeforeTheOfflineFromGivingUp =
        ServiceLifecycleManagerImpl.MAX_RECONNECTION_CYCLES

      serviceLifecycleManager.onConnectionLost()
      advanceThroughAllReconnectionCycles()

      coVerify(exactly = attemptsBeforeTheOfflineFromGivingUp) {
        connectionUseCase.connect(any(), any())
      }
    }
  }

  @Test
  fun `manual reconnect clears the stop pending latch left by a give up`() {
    runTest(testDispatcher) {
      ServiceState.setRunning(true)
      serviceLifecycleManager.onConnectionLost()
      advanceThroughAllReconnectionCycles()
      assertThat(serviceLifecycleManager.isStopPending).isTrue()

      serviceLifecycleManager.onManualReconnect()

      assertThat(serviceLifecycleManager.isStopPending).isFalse()
    }
  }

  @Test
  fun `connection lost after a disconnect then a manual reconnect still reconnects`() {
    runTest(testDispatcher) {
      ServiceState.setRunning(true)
      serviceLifecycleManager.onIntentionalDisconnect()

      serviceLifecycleManager.onManualReconnect()

      ServiceState.setRunning(true)
      serviceLifecycleManager.onConnectionLost()
      testDispatcher.scheduler.advanceTimeBy(
        firstBackoff + 1000
      )

      coVerify(atLeast = 1) { connectionUseCase.connect(any(), any()) }
    }
  }

  @Test
  fun isStopPendingShouldBeFalseInitially() {
    runTest(testDispatcher) {
      // Then
      assertThat(serviceLifecycleManager.isStopPending).isFalse()
    }
  }

  @Test
  fun connectionRestoredDuringReconnectionDelayShouldPreventConnect() {
    runTest(testDispatcher) {
      // Given - service is running
      ServiceState.setRunning(true)
      serviceLifecycleManager.onConnectionLost()

      // Advance partway through the first delay
      testDispatcher.scheduler.advanceTimeBy(firstBackoff / 2)

      // When - connection is restored during the delay
      serviceLifecycleManager.onConnectionRestored()

      // Advance past what would have been the reconnection time
      testDispatcher.scheduler.advanceTimeBy(totalBackoff())

      // Then - no connect calls should have been made
      coVerify(exactly = 0) { connectionUseCase.connect(any(), any()) }
      verify(exactly = 0) { application.stopService(any()) }
    }
  }

  @Test
  fun connectionRestoredAfterFirstCycleShouldStopFurtherAttempts() {
    runTest(testDispatcher) {
      // Given - service is running
      ServiceState.setRunning(true)
      serviceLifecycleManager.onConnectionLost()

      testDispatcher.scheduler.advanceTimeBy(firstBackoff + 100)

      // Verify first connect was called
      coVerify(exactly = 1) { connectionUseCase.connect(any(), any()) }

      // When - connection is restored after first cycle
      serviceLifecycleManager.onConnectionRestored()

      // Advance through remaining cycles
      testDispatcher.scheduler.advanceTimeBy(totalBackoff())

      // Then - no additional connect calls should have been made
      coVerify(exactly = 1) { connectionUseCase.connect(any(), any()) }
      verify(exactly = 0) { application.stopService(any()) }
    }
  }

  @Test
  fun newConnectionLostAfterRestoredShouldStartNewReconnectionLoop() {
    runTest(testDispatcher) {
      // Given - service is running
      ServiceState.setRunning(true)

      // First connection loss and restore
      serviceLifecycleManager.onConnectionLost()
      testDispatcher.scheduler.advanceTimeBy(firstBackoff / 2)
      serviceLifecycleManager.onConnectionRestored()
      testDispatcher.scheduler.advanceUntilIdle()

      // Verify no connects happened
      coVerify(exactly = 0) { connectionUseCase.connect(any(), any()) }

      // When - new connection loss occurs
      serviceLifecycleManager.onConnectionLost()

      // Advance past first delay
      testDispatcher.scheduler.advanceTimeBy(
        firstBackoff + 100
      )

      // Then - new reconnection attempt should be made
      coVerify(exactly = 1) { connectionUseCase.connect(any(), any()) }
    }
  }

  private fun advanceThroughAllReconnectionCycles() {
    testDispatcher.scheduler.advanceTimeBy(totalBackoff() + 1000)
  }
}
