package com.kelsos.mbrc.networking

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import com.kelsos.mbrc.utils.testDispatcher
import com.kelsos.mbrc.utils.testDispatcherModule
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.test.KoinTest
import org.koin.test.inject

@RunWith(AndroidJUnit4::class)
class SocketActivityCheckerTest : KoinTest {
  private val testModule = testDispatcherModule

  private val dispatchers by inject<com.kelsos.mbrc.common.utilities.AppCoroutineDispatchers>()
  private lateinit var activityChecker: SocketActivityChecker

  @Before
  fun setUp() {
    startKoin {
      modules(listOf(testModule))
    }
    activityChecker = SocketActivityChecker(dispatchers)
  }

  @After
  fun tearDown() {
    activityChecker.stop()
    stopKoin()
  }

  @Test
  fun startShouldInitializeActivityChecker() {
    runTest(testDispatcher) {
      // When
      activityChecker.start()
      delay(100) // Allow start to process

      // Then
      assertThat(activityChecker.isHealthy()).isTrue()
      assertThat(activityChecker.getTimeoutCount()).isEqualTo(0)
    }
  }

  @Test
  fun stopShouldStopActivityChecker() {
    runTest(testDispatcher) {
      // Given
      activityChecker.start()
      delay(100)

      // When
      activityChecker.stop()
      delay(100)

      // Then
      assertThat(activityChecker.isHealthy()).isFalse()
    }
  }

  @Test
  fun pingShouldResetTimeout() {
    runTest(testDispatcher) {
      // Given
      activityChecker.start()
      delay(100)

      // When
      activityChecker.ping()
      delay(100)

      // Then
      assertThat(activityChecker.getTimeoutCount()).isEqualTo(0)
      assertThat(activityChecker.isHealthy()).isTrue()
    }
  }

  @Test
  fun pingWhenNotRunningShouldBeIgnored() {
    runTest(testDispatcher) {
      // Given - activity checker not started

      // When
      activityChecker.ping()
      delay(100)

      // Then
      assertThat(activityChecker.isHealthy()).isFalse()
    }
  }

  @Test
  fun timeoutShouldCallListener() {
    runTest(testDispatcher) {
      // Given
      val mockListener: () -> Unit = mockk(relaxed = true)
      activityChecker.setPingTimeoutListener(mockListener)
      activityChecker.start()

      // When - advance time past the timeout (40s)
      testDispatcher.scheduler.advanceTimeBy(41_000L)

      // Then
      verify { mockListener() }
    }
  }

  @Test
  fun multipleStartsShouldNotCauseIssues() {
    runTest(testDispatcher) {
      // When
      activityChecker.start()
      activityChecker.start()
      activityChecker.start()
      delay(100)

      // Then
      assertThat(activityChecker.isHealthy()).isTrue()
    }
  }

  @Test
  fun multipleStopsShouldNotCauseIssues() {
    runTest(testDispatcher) {
      // Given
      activityChecker.start()
      delay(100)

      // When
      activityChecker.stop()
      activityChecker.stop()
      activityChecker.stop()
      delay(100)

      // Then
      assertThat(activityChecker.isHealthy()).isFalse()
    }
  }
}
