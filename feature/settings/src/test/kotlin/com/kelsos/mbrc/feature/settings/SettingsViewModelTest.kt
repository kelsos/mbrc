package com.kelsos.mbrc.feature.settings

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import com.kelsos.mbrc.core.common.settings.TrackAction
import com.kelsos.mbrc.core.common.test.testDispatcher
import com.kelsos.mbrc.core.common.test.testDispatcherModule
import com.kelsos.mbrc.core.platform.service.ServiceRestarter
import com.kelsos.mbrc.feature.settings.data.CallAction
import com.kelsos.mbrc.feature.settings.domain.SettingsManager
import com.kelsos.mbrc.feature.settings.theme.Theme
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.dsl.module
import org.koin.test.KoinTest
import org.robolectric.RobolectricTestRunner

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class SettingsViewModelTest : KoinTest {

  private lateinit var viewModel: SettingsViewModel
  private lateinit var settingsManager: SettingsManager
  private lateinit var serviceRestarter: ServiceRestarter

  private val themeFlow = MutableStateFlow<Theme>(Theme.System)
  private val pluginUpdateCheckFlow = MutableStateFlow(false)
  private val debugLoggingFlow = MutableStateFlow(false)
  private val incomingCallActionFlow = MutableStateFlow<CallAction>(CallAction.None)
  private val trackDefaultActionFlow = MutableStateFlow<TrackAction>(TrackAction.PlayNow)
  private val halfStarRatingFlow = MutableStateFlow(false)
  private val showRatingOnPlayerFlow = MutableStateFlow(false)

  private val testModule = module {
    single { settingsManager }
    single { serviceRestarter }
  }

  @Before
  fun setUp() {
    settingsManager = mockk(relaxed = true) {
      every { themeFlow } returns this@SettingsViewModelTest.themeFlow
      every { pluginUpdateCheckFlow } returns this@SettingsViewModelTest.pluginUpdateCheckFlow
      every { debugLoggingFlow } returns this@SettingsViewModelTest.debugLoggingFlow
      every { incomingCallActionFlow } returns this@SettingsViewModelTest.incomingCallActionFlow
      every { libraryTrackDefaultActionFlow } returns
        this@SettingsViewModelTest.trackDefaultActionFlow
      every { halfStarRatingFlow } returns this@SettingsViewModelTest.halfStarRatingFlow
      every { showRatingOnPlayerFlow } returns this@SettingsViewModelTest.showRatingOnPlayerFlow
    }
    serviceRestarter = mockk(relaxed = true)

    startKoin { modules(listOf(testModule, testDispatcherModule)) }

    viewModel = SettingsViewModel(settingsManager, serviceRestarter)
  }

  @After
  fun tearDown() {
    stopKoin()
  }

  @Test
  fun `initial state should have default values`() = runTest(testDispatcher) {
    viewModel.currentTheme.test {
      assertThat(awaitItem()).isEqualTo(Theme.System)
    }

    viewModel.pluginUpdatesEnabled.test {
      assertThat(awaitItem()).isFalse()
    }

    viewModel.debugLoggingEnabled.test {
      assertThat(awaitItem()).isFalse()
    }

    viewModel.incomingCallAction.test {
      assertThat(awaitItem()).isEqualTo(CallAction.None)
    }

    viewModel.trackDefaultAction.test {
      assertThat(awaitItem()).isEqualTo(TrackAction.PlayNow)
    }

    viewModel.visibleDialog.test {
      assertThat(awaitItem()).isNull()
    }
  }

  @Test
  fun `updateTheme should call settingsManager setTheme`() = runTest(testDispatcher) {
    coEvery { settingsManager.setTheme(any()) } returns Unit

    viewModel.updateTheme(Theme.Dark)
    advanceUntilIdle()

    coVerify { settingsManager.setTheme(Theme.Dark) }
  }

  @Test
  fun `updatePluginUpdates should call settingsManager setPluginUpdateCheck`() =
    runTest(testDispatcher) {
      coEvery { settingsManager.setPluginUpdateCheck(any()) } returns Unit

      viewModel.updatePluginUpdates(true)
      advanceUntilIdle()

      coVerify { settingsManager.setPluginUpdateCheck(true) }
    }

  @Test
  fun `updateDebugLogging only stores the preference`() = runTest(testDispatcher) {
    coEvery { settingsManager.setDebugLogging(any()) } returns Unit

    viewModel.updateDebugLogging(true)
    advanceUntilIdle()

    coVerify { settingsManager.setDebugLogging(true) }
  }

  @Test
  fun `updateIncomingCallAction should call settingsManager and restart service`() =
    runTest(testDispatcher) {
      coEvery { settingsManager.setIncomingCallAction(any()) } returns Unit

      viewModel.updateIncomingCallAction(CallAction.Pause)
      advanceUntilIdle()

      coVerify { settingsManager.setIncomingCallAction(CallAction.Pause) }
      verify { serviceRestarter.restartService() }
    }

  @Test
  fun `updateTrackDefaultAction should call settingsManager setLibraryTrackDefaultAction`() =
    runTest(testDispatcher) {
      coEvery { settingsManager.setLibraryTrackDefaultAction(any()) } returns Unit

      viewModel.updateTrackDefaultAction(TrackAction.QueueNext)
      advanceUntilIdle()

      coVerify { settingsManager.setLibraryTrackDefaultAction(TrackAction.QueueNext) }
    }

  @Test
  fun `showDialog should update visibleDialog state`() = runTest(testDispatcher) {
    viewModel.visibleDialog.test {
      assertThat(awaitItem()).isNull()

      viewModel.showDialog(SettingsDialogType.Theme)
      assertThat(awaitItem()).isEqualTo(SettingsDialogType.Theme)
    }
  }

  @Test
  fun `showDialog with different types should update correctly`() = runTest(testDispatcher) {
    viewModel.visibleDialog.test {
      assertThat(awaitItem()).isNull()

      viewModel.showDialog(SettingsDialogType.IncomingCallAction)
      assertThat(awaitItem()).isEqualTo(SettingsDialogType.IncomingCallAction)

      viewModel.showDialog(SettingsDialogType.TrackDefaultAction)
      assertThat(awaitItem()).isEqualTo(SettingsDialogType.TrackDefaultAction)
    }
  }

  @Test
  fun `hideDialog should set visibleDialog to null`() = runTest(testDispatcher) {
    viewModel.visibleDialog.test {
      assertThat(awaitItem()).isNull()

      viewModel.showDialog(SettingsDialogType.Theme)
      assertThat(awaitItem()).isEqualTo(SettingsDialogType.Theme)

      viewModel.hideDialog()
      assertThat(awaitItem()).isNull()
    }
  }

  @Test
  fun `theme flow should emit new values when source flow changes`() = runTest(testDispatcher) {
    viewModel.currentTheme.test {
      assertThat(awaitItem()).isEqualTo(Theme.System)

      themeFlow.value = Theme.Dark
      assertThat(awaitItem()).isEqualTo(Theme.Dark)

      themeFlow.value = Theme.Light
      assertThat(awaitItem()).isEqualTo(Theme.Light)
    }
  }

  @Test
  fun `pluginUpdatesEnabled flow should emit new values when source flow changes`() =
    runTest(testDispatcher) {
      viewModel.pluginUpdatesEnabled.test {
        assertThat(awaitItem()).isFalse()

        pluginUpdateCheckFlow.value = true
        assertThat(awaitItem()).isTrue()
      }
    }

  @Test
  fun `debugLoggingEnabled flow should emit new values when source flow changes`() =
    runTest(testDispatcher) {
      viewModel.debugLoggingEnabled.test {
        assertThat(awaitItem()).isFalse()

        debugLoggingFlow.value = true
        assertThat(awaitItem()).isTrue()
      }
    }

  @Test
  fun `incomingCallAction flow should emit new values when source flow changes`() =
    runTest(testDispatcher) {
      viewModel.incomingCallAction.test {
        assertThat(awaitItem()).isEqualTo(CallAction.None)

        incomingCallActionFlow.value = CallAction.Pause
        assertThat(awaitItem()).isEqualTo(CallAction.Pause)

        incomingCallActionFlow.value = CallAction.Stop
        assertThat(awaitItem()).isEqualTo(CallAction.Stop)
      }
    }

  @Test
  fun `trackDefaultAction flow should emit new values when source flow changes`() =
    runTest(testDispatcher) {
      viewModel.trackDefaultAction.test {
        assertThat(awaitItem()).isEqualTo(TrackAction.PlayNow)

        trackDefaultActionFlow.value = TrackAction.QueueLast
        assertThat(awaitItem()).isEqualTo(TrackAction.QueueLast)
      }
    }

  @Test
  fun `initial halfStarRatingEnabled should be false`() = runTest(testDispatcher) {
    viewModel.halfStarRatingEnabled.test {
      assertThat(awaitItem()).isFalse()
    }
  }

  @Test
  fun `initial showRatingOnPlayerEnabled should be false`() = runTest(testDispatcher) {
    viewModel.showRatingOnPlayerEnabled.test {
      assertThat(awaitItem()).isFalse()
    }
  }

  @Test
  fun `updateHalfStarRating should call settingsManager setHalfStarRating`() =
    runTest(testDispatcher) {
      coEvery { settingsManager.setHalfStarRating(any()) } returns Unit

      viewModel.updateHalfStarRating(true)
      advanceUntilIdle()

      coVerify { settingsManager.setHalfStarRating(true) }
    }

  @Test
  fun `updateHalfStarRating with false should call settingsManager`() = runTest(testDispatcher) {
    coEvery { settingsManager.setHalfStarRating(any()) } returns Unit

    viewModel.updateHalfStarRating(false)
    advanceUntilIdle()

    coVerify { settingsManager.setHalfStarRating(false) }
  }

  @Test
  fun `updateShowRatingOnPlayer should call settingsManager setShowRatingOnPlayer`() =
    runTest(testDispatcher) {
      coEvery { settingsManager.setShowRatingOnPlayer(any()) } returns Unit

      viewModel.updateShowRatingOnPlayer(true)
      advanceUntilIdle()

      coVerify { settingsManager.setShowRatingOnPlayer(true) }
    }

  @Test
  fun `updateShowRatingOnPlayer with false should call settingsManager`() =
    runTest(testDispatcher) {
      coEvery { settingsManager.setShowRatingOnPlayer(any()) } returns Unit

      viewModel.updateShowRatingOnPlayer(false)
      advanceUntilIdle()

      coVerify { settingsManager.setShowRatingOnPlayer(false) }
    }

  @Test
  fun `halfStarRatingEnabled flow should emit new values when source flow changes`() =
    runTest(testDispatcher) {
      viewModel.halfStarRatingEnabled.test {
        assertThat(awaitItem()).isFalse()

        halfStarRatingFlow.value = true
        assertThat(awaitItem()).isTrue()

        halfStarRatingFlow.value = false
        assertThat(awaitItem()).isFalse()
      }
    }

  @Test
  fun `showRatingOnPlayerEnabled flow should emit new values when source flow changes`() =
    runTest(testDispatcher) {
      viewModel.showRatingOnPlayerEnabled.test {
        assertThat(awaitItem()).isFalse()

        showRatingOnPlayerFlow.value = true
        assertThat(awaitItem()).isTrue()

        showRatingOnPlayerFlow.value = false
        assertThat(awaitItem()).isFalse()
      }
    }
}
