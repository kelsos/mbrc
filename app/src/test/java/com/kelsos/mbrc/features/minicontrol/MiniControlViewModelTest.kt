package com.kelsos.mbrc.features.minicontrol

import androidx.test.ext.junit.runners.AndroidJUnit4
import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import com.kelsos.mbrc.common.state.AppStateFlow
import com.kelsos.mbrc.common.state.ConnectionStateFlow
import com.kelsos.mbrc.common.state.PlayerState
import com.kelsos.mbrc.common.state.PlayerStatusModel
import com.kelsos.mbrc.common.state.PlayingPosition
import com.kelsos.mbrc.common.state.PlayingTrack
import com.kelsos.mbrc.networking.protocol.UserActionUseCase
import com.kelsos.mbrc.networking.protocol.next
import com.kelsos.mbrc.networking.protocol.playPause
import com.kelsos.mbrc.networking.protocol.previous
import com.kelsos.mbrc.utils.testDispatcher
import com.kelsos.mbrc.utils.testDispatcherModule
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
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
import java.io.IOException

@RunWith(AndroidJUnit4::class)
class MiniControlViewModelTest : KoinTest {
  private val testModule =
    module {
      single<AppStateFlow> { mockk(relaxed = true) }
      single<UserActionUseCase> { mockk(relaxed = true) }
      single<ConnectionStateFlow> { mockk(relaxed = true) }
      singleOf(::MiniControlViewModel)
    }

  private val viewModel: MiniControlViewModel by inject()
  private val appStateFlow: AppStateFlow by inject()
  private val userActionUseCase: UserActionUseCase by inject()
  private val connectionStateFlow: ConnectionStateFlow by inject()

  @Before
  fun setUp() {
    startKoin {
      modules(listOf(testModule, testDispatcherModule))
    }

    // Setup default mocks
    val playingTrack =
      PlayingTrack(
        artist = "Test Artist",
        title = "Test Title",
        album = "Test Album",
        path = "test/path",
        coverUrl = "test/cover/url",
      )
    val playingPosition = PlayingPosition(current = 30, total = 100)
    val playerStatus =
      PlayerStatusModel(
        mute = false,
        state = PlayerState.Playing,
        volume = 50,
      )

    every { appStateFlow.playingTrack } returns MutableStateFlow(playingTrack)
    every { appStateFlow.playingPosition } returns MutableStateFlow(playingPosition)
    every { appStateFlow.playerStatus } returns MutableStateFlow(playerStatus)
    coEvery { connectionStateFlow.isConnected() } returns true
  }

  @After
  fun tearDown() {
    stopKoin()
  }

  @Test
  fun stateShouldCombineAppStateFlows() {
    runTest(testDispatcher) {
      // Given
      val expectedTrack =
        PlayingTrack(
          artist = "Artist",
          title = "Title",
          album = "Album",
          path = "path",
          coverUrl = "cover",
        )
      val expectedPosition = PlayingPosition(current = 50, total = 200)
      val expectedPlayerStatus =
        PlayerStatusModel(
          mute = false,
          state = PlayerState.Paused,
          volume = 50,
        )

      every { appStateFlow.playingTrack } returns MutableStateFlow(expectedTrack)
      every { appStateFlow.playingPosition } returns MutableStateFlow(expectedPosition)
      every { appStateFlow.playerStatus } returns MutableStateFlow(expectedPlayerStatus)

      // Then
      viewModel.state.test {
        val state = awaitItem()
        assertThat(state.playingTrack).isEqualTo(expectedTrack)
        assertThat(state.playingPosition).isEqualTo(expectedPosition)
        assertThat(state.playingState).isEqualTo(PlayerState.Paused)
      }
    }
  }

  @Test
  fun performPlayPauseShouldEmitNetworkUnavailableWhenNotConnected() {
    runTest(testDispatcher) {
      // Given
      coEvery { connectionStateFlow.isConnected() } returns false

      // When & Then
      viewModel.events.test {
        viewModel.perform(MiniControlAction.PlayPause)
        testDispatcher.scheduler.advanceUntilIdle()

        val event = awaitItem()
        assertThat(event).isEqualTo(MiniControlUiMessages.NetworkUnavailable)
      }

      // Verify user action is not called when not connected
      coVerify(exactly = 0) { userActionUseCase.playPause() }
    }
  }

  @Test
  fun performPlayPauseShouldNotEmitWhenConnectedAndUserActionSucceeds() {
    runTest(testDispatcher) {
      // Given
      coEvery { connectionStateFlow.isConnected() } returns true
      coEvery { userActionUseCase.playPause() } returns Unit

      // When & Then
      viewModel.events.test {
        viewModel.perform(MiniControlAction.PlayPause)
        testDispatcher.scheduler.advanceUntilIdle()

        // Should not emit any events on successful action
        expectNoEvents()
      }

      // Verify user action was called
      coVerify(exactly = 1) { userActionUseCase.playPause() }
    }
  }

  @Test
  fun performPlayPauseShouldEmitActionFailedWhenConnectedButUserActionThrowsIOException() {
    runTest(testDispatcher) {
      // Given
      coEvery { connectionStateFlow.isConnected() } returns true
      coEvery { userActionUseCase.playPause() } throws IOException("Network error")

      // When & Then
      viewModel.events.test {
        viewModel.perform(MiniControlAction.PlayPause)
        testDispatcher.scheduler.advanceUntilIdle()

        val event = awaitItem()
        assertThat(event).isEqualTo(MiniControlUiMessages.ActionFailed)
      }

      // Verify user action was called
      coVerify(exactly = 1) { userActionUseCase.playPause() }
    }
  }

  @Test
  fun performPlayNextShouldEmitNetworkUnavailableWhenNotConnected() {
    runTest(testDispatcher) {
      // Given
      coEvery { connectionStateFlow.isConnected() } returns false

      // When & Then
      viewModel.events.test {
        viewModel.perform(MiniControlAction.PlayNext)
        testDispatcher.scheduler.advanceUntilIdle()

        val event = awaitItem()
        assertThat(event).isEqualTo(MiniControlUiMessages.NetworkUnavailable)
      }

      // Verify user action is not called when not connected
      coVerify(exactly = 0) { userActionUseCase.next() }
    }
  }

  @Test
  fun performPlayNextShouldNotEmitWhenConnectedAndUserActionSucceeds() {
    runTest(testDispatcher) {
      // Given
      coEvery { connectionStateFlow.isConnected() } returns true
      coEvery { userActionUseCase.next() } returns Unit

      // When & Then
      viewModel.events.test {
        viewModel.perform(MiniControlAction.PlayNext)
        testDispatcher.scheduler.advanceUntilIdle()

        // Should not emit any events on successful action
        expectNoEvents()
      }

      // Verify user action was called
      coVerify(exactly = 1) { userActionUseCase.next() }
    }
  }

  @Test
  fun performPlayNextShouldEmitActionFailedWhenConnectedButUserActionThrowsIOException() {
    runTest(testDispatcher) {
      // Given
      coEvery { connectionStateFlow.isConnected() } returns true
      coEvery { userActionUseCase.next() } throws IOException("Network error")

      // When & Then
      viewModel.events.test {
        viewModel.perform(MiniControlAction.PlayNext)
        testDispatcher.scheduler.advanceUntilIdle()

        val event = awaitItem()
        assertThat(event).isEqualTo(MiniControlUiMessages.ActionFailed)
      }

      // Verify user action was called
      coVerify(exactly = 1) { userActionUseCase.next() }
    }
  }

  @Test
  fun performPlayPreviousShouldEmitNetworkUnavailableWhenNotConnected() {
    runTest(testDispatcher) {
      // Given
      coEvery { connectionStateFlow.isConnected() } returns false

      // When & Then
      viewModel.events.test {
        viewModel.perform(MiniControlAction.PlayPrevious)
        testDispatcher.scheduler.advanceUntilIdle()

        val event = awaitItem()
        assertThat(event).isEqualTo(MiniControlUiMessages.NetworkUnavailable)
      }

      // Verify user action is not called when not connected
      coVerify(exactly = 0) { userActionUseCase.previous() }
    }
  }

  @Test
  fun performPlayPreviousShouldNotEmitWhenConnectedAndUserActionSucceeds() {
    runTest(testDispatcher) {
      // Given
      coEvery { connectionStateFlow.isConnected() } returns true
      coEvery { userActionUseCase.previous() } returns Unit

      // When & Then
      viewModel.events.test {
        viewModel.perform(MiniControlAction.PlayPrevious)
        testDispatcher.scheduler.advanceUntilIdle()

        // Should not emit any events on successful action
        expectNoEvents()
      }

      // Verify user action was called
      coVerify(exactly = 1) { userActionUseCase.previous() }
    }
  }

  @Test
  fun performPlayPreviousShouldEmitActionFailedWhenConnectedButUserActionThrowsIOException() {
    runTest(testDispatcher) {
      // Given
      coEvery { connectionStateFlow.isConnected() } returns true
      coEvery { userActionUseCase.previous() } throws IOException("Network error")

      // When & Then
      viewModel.events.test {
        viewModel.perform(MiniControlAction.PlayPrevious)
        testDispatcher.scheduler.advanceUntilIdle()

        val event = awaitItem()
        assertThat(event).isEqualTo(MiniControlUiMessages.ActionFailed)
      }

      // Verify user action was called
      coVerify(exactly = 1) { userActionUseCase.previous() }
    }
  }

  @Test
  fun multipleActionsShouldBehaveCorrectly() {
    runTest(testDispatcher) {
      // Given
      coEvery { connectionStateFlow.isConnected() } returns true
      coEvery { userActionUseCase.playPause() } returns Unit
      coEvery { userActionUseCase.next() } returns Unit
      coEvery { userActionUseCase.previous() } returns Unit

      // When & Then
      viewModel.events.test {
        viewModel.perform(MiniControlAction.PlayPause)
        viewModel.perform(MiniControlAction.PlayNext)
        viewModel.perform(MiniControlAction.PlayPrevious)
        testDispatcher.scheduler.advanceUntilIdle()

        // Should not emit any events on successful actions
        expectNoEvents()
      }

      // Verify all user actions were called
      coVerify(exactly = 1) { userActionUseCase.playPause() }
      coVerify(exactly = 1) { userActionUseCase.next() }
      coVerify(exactly = 1) { userActionUseCase.previous() }
    }
  }

  @Test
  fun networkCheckIsPerformedAtStartOfEachOperation() {
    runTest(testDispatcher) {
      // Given - connection starts as true, then becomes false
      coEvery { connectionStateFlow.isConnected() } returns true andThen false
      coEvery { userActionUseCase.playPause() } returns Unit

      // When & Then - First call should succeed, second should fail
      viewModel.events.test {
        viewModel.perform(MiniControlAction.PlayPause) // Should succeed (first call)
        testDispatcher.scheduler.advanceUntilIdle()

        expectNoEvents() // No events on success

        viewModel.perform(MiniControlAction.PlayPause) // Should fail (second call)
        testDispatcher.scheduler.advanceUntilIdle()

        val event = awaitItem()
        assertThat(event).isEqualTo(MiniControlUiMessages.NetworkUnavailable)
      }

      // Verify user action was called only once (for successful call)
      coVerify(exactly = 1) { userActionUseCase.playPause() }
    }
  }

  @Test
  fun concurrentActionsShouldHandleNetworkFailuresIndependently() {
    runTest(testDispatcher) {
      // Given
      coEvery { connectionStateFlow.isConnected() } returns true
      coEvery { userActionUseCase.playPause() } throws IOException("Network error")
      coEvery { userActionUseCase.next() } returns Unit

      // When & Then
      viewModel.events.test {
        viewModel.perform(MiniControlAction.PlayPause)
        viewModel.perform(MiniControlAction.PlayNext)
        testDispatcher.scheduler.advanceUntilIdle()

        // Should only emit one ActionFailed event (for PlayPause)
        val event = awaitItem()
        assertThat(event).isEqualTo(MiniControlUiMessages.ActionFailed)
        expectNoEvents()
      }

      // Verify both user actions were called
      coVerify(exactly = 1) { userActionUseCase.playPause() }
      coVerify(exactly = 1) { userActionUseCase.next() }
    }
  }
}
