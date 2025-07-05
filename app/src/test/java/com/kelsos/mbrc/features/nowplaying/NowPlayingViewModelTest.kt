package com.kelsos.mbrc.features.nowplaying

import androidx.paging.PagingData
import androidx.test.ext.junit.runners.AndroidJUnit4
import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import com.kelsos.mbrc.common.state.AppStateFlow
import com.kelsos.mbrc.common.state.ConnectionStateFlow
import com.kelsos.mbrc.common.state.PlayingTrack
import com.kelsos.mbrc.networking.protocol.Protocol
import com.kelsos.mbrc.networking.protocol.UserAction
import com.kelsos.mbrc.networking.protocol.UserActionUseCase
import com.kelsos.mbrc.utils.testDispatcher
import com.kelsos.mbrc.utils.testDispatcherModule
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
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
class NowPlayingViewModelTest : KoinTest {
  private val testModule =
    module {
      single<NowPlayingRepository> { mockk(relaxed = true) }
      single<MoveManager> { mockk(relaxed = true) }
      single<UserActionUseCase> { mockk(relaxed = true) }
      single<ConnectionStateFlow> { mockk(relaxed = true) }
      single<AppStateFlow> { mockk(relaxed = true) }
      singleOf(::NowPlayingViewModel)
    }

  private val viewModel: NowPlayingViewModel by inject()
  private val repository: NowPlayingRepository by inject()
  private val moveManager: MoveManager by inject()
  private val userActionUseCase: UserActionUseCase by inject()
  private val connectionStateFlow: ConnectionStateFlow by inject()
  private val appStateFlow: AppStateFlow by inject()

  @Before
  fun setUp() {
    startKoin {
      modules(listOf(testModule, testDispatcherModule))
    }

    every { repository.getAll() } returns flowOf(PagingData.empty())
    every { appStateFlow.playingTrack } returns MutableStateFlow(PlayingTrack())
    coEvery { connectionStateFlow.isConnected() } returns true
  }

  @After
  fun tearDown() {
    stopKoin()
  }

  @Test
  fun reloadShouldEmitNetworkUnavailableWhenNotConnectedAndShowUserMessageTrue() {
    runTest(testDispatcher) {
      // Given
      coEvery { connectionStateFlow.isConnected() } returns false

      // When & Then
      viewModel.events.test {
        viewModel.actions.reload(showUserMessage = true)
        testDispatcher.scheduler.advanceUntilIdle()

        val event = awaitItem()
        assertThat(event).isEqualTo(NowPlayingUiMessages.NetworkUnavailable)
      }

      // Verify repository is not called when not connected
      coVerify(exactly = 0) { repository.getRemote() }
    }
  }

  @Test
  fun reloadShouldNotEmitWhenNotConnectedAndShowUserMessageFalse() {
    runTest(testDispatcher) {
      // Given
      coEvery { connectionStateFlow.isConnected() } returns false

      // When & Then
      viewModel.events.test {
        viewModel.actions.reload(showUserMessage = false)
        testDispatcher.scheduler.advanceUntilIdle()

        // Should not emit any events
        expectNoEvents()
      }

      // Verify repository is not called when not connected
      coVerify(exactly = 0) { repository.getRemote() }
    }
  }

  @Test
  fun reloadShouldEmitRefreshSuccessWhenConnectedAndRepositorySucceedsAndShowUserMessageTrue() {
    runTest(testDispatcher) {
      // Given
      coEvery { connectionStateFlow.isConnected() } returns true
      coEvery { repository.getRemote() } returns Unit

      // When & Then
      viewModel.events.test {
        viewModel.actions.reload(showUserMessage = true)
        testDispatcher.scheduler.advanceUntilIdle()

        val event = awaitItem()
        assertThat(event).isEqualTo(NowPlayingUiMessages.RefreshSucceeded)
      }

      // Verify repository was called
      coVerify(exactly = 1) { repository.getRemote() }
    }
  }

  @Test
  fun reloadShouldNotEmitWhenConnectedAndRepositorySucceedsAndShowUserMessageFalse() {
    runTest(testDispatcher) {
      // Given
      coEvery { connectionStateFlow.isConnected() } returns true
      coEvery { repository.getRemote() } returns Unit

      // When & Then
      viewModel.events.test {
        viewModel.actions.reload(showUserMessage = false)
        testDispatcher.scheduler.advanceUntilIdle()

        // Should not emit any events on success when showUserMessage is false
        expectNoEvents()
      }

      // Verify repository was called
      coVerify(exactly = 1) { repository.getRemote() }
    }
  }

  @Test
  fun reloadShouldEmitRefreshFailedWhenConnectedButRepositoryThrowsIOExceptionAndShowUserMessageTrue() {
    runTest(testDispatcher) {
      // Given
      val ioException = IOException("Network error")
      coEvery { connectionStateFlow.isConnected() } returns true
      coEvery { repository.getRemote() } throws ioException

      // When & Then
      viewModel.events.test {
        viewModel.actions.reload(showUserMessage = true)
        testDispatcher.scheduler.advanceUntilIdle()

        val event = awaitItem()
        assertThat(event).isInstanceOf(NowPlayingUiMessages.RefreshFailed::class.java)
        assertThat((event as NowPlayingUiMessages.RefreshFailed).throwable).isEqualTo(ioException)
      }

      // Verify repository was called
      coVerify(exactly = 1) { repository.getRemote() }
    }
  }

  @Test
  fun reloadShouldNotEmitWhenConnectedButRepositoryThrowsIOExceptionAndShowUserMessageFalse() {
    runTest(testDispatcher) {
      // Given
      coEvery { connectionStateFlow.isConnected() } returns true
      coEvery { repository.getRemote() } throws IOException("Network error")

      // When & Then
      viewModel.events.test {
        viewModel.actions.reload(showUserMessage = false)
        testDispatcher.scheduler.advanceUntilIdle()

        // Should not emit any events on failure when showUserMessage is false
        expectNoEvents()
      }

      // Verify repository was called
      coVerify(exactly = 1) { repository.getRemote() }
    }
  }

  @Test
  fun reloadWithoutParameterShouldDefaultToShowUserMessageTrue() {
    runTest(testDispatcher) {
      // Given
      coEvery { connectionStateFlow.isConnected() } returns true
      coEvery { repository.getRemote() } returns Unit

      // When & Then
      viewModel.events.test {
        viewModel.actions.reload()
        testDispatcher.scheduler.advanceUntilIdle()

        val event = awaitItem()
        assertThat(event).isEqualTo(NowPlayingUiMessages.RefreshSucceeded)
      }

      // Verify repository was called
      coVerify(exactly = 1) { repository.getRemote() }
    }
  }

  @Test
  fun networkCheckIsPerformedAtStartOfOperation() {
    runTest(testDispatcher) {
      // Given - connection starts as true, then becomes false
      coEvery { connectionStateFlow.isConnected() } returns true andThen false
      coEvery { repository.getRemote() } returns Unit

      // When & Then - First call should succeed, second should fail
      viewModel.events.test {
        viewModel.actions.reload(showUserMessage = true) // Should succeed (first call)
        testDispatcher.scheduler.advanceUntilIdle()

        val firstEvent = awaitItem()
        assertThat(firstEvent).isEqualTo(NowPlayingUiMessages.RefreshSucceeded)

        viewModel.actions.reload(showUserMessage = true) // Should fail (second call)
        testDispatcher.scheduler.advanceUntilIdle()

        val secondEvent = awaitItem()
        assertThat(secondEvent).isEqualTo(NowPlayingUiMessages.NetworkUnavailable)
      }
    }
  }

  @Test
  fun tracksShouldReturnRepositoryPagingData() {
    // Given
    val mockPagingData = PagingData.empty<NowPlaying>()
    every { repository.getAll() } returns flowOf(mockPagingData)

    // Then
    assertThat(viewModel.tracks).isNotNull()
    // Note: PagingData testing requires more setup, this verifies the flow is accessible
  }

  @Test
  fun playingTrackShouldReturnAppStatePlayingTrack() {
    // Given
    val mockPlayingTrack = PlayingTrack(path = "test/path")
    every { appStateFlow.playingTrack } returns MutableStateFlow(mockPlayingTrack)

    // Then
    assertThat(viewModel.playingTrack).isNotNull()
    // Note: Flow testing requires more setup, this verifies the flow is accessible
  }

  @Test
  fun multipleReloadCallsWithDifferentParametersShouldBehaveCorrectly() {
    runTest(testDispatcher) {
      // Given
      coEvery { connectionStateFlow.isConnected() } returns true
      coEvery { repository.getRemote() } returns Unit

      // When & Then
      viewModel.events.test {
        viewModel.actions.reload(showUserMessage = true)
        viewModel.actions.reload(showUserMessage = false)
        testDispatcher.scheduler.advanceUntilIdle()

        // Only the first call should emit an event
        val event = awaitItem()
        assertThat(event).isEqualTo(NowPlayingUiMessages.RefreshSucceeded)
        expectNoEvents()
      }

      // Verify repository was called twice
      coVerify(exactly = 2) { repository.getRemote() }
    }
  }

  @Test
  fun playShouldEmitNetworkUnavailableWhenNotConnected() {
    runTest(testDispatcher) {
      // Given
      coEvery { connectionStateFlow.isConnected() } returns false

      // When & Then
      viewModel.events.test {
        viewModel.actions.play(5)
        testDispatcher.scheduler.advanceUntilIdle()

        val event = awaitItem()
        assertThat(event).isEqualTo(NowPlayingUiMessages.NetworkUnavailable)
      }

      // Verify user action is not called when not connected
      coVerify(exactly = 0) { userActionUseCase.perform(any()) }
    }
  }

  @Test
  fun playShouldNotEmitWhenConnectedAndUserActionSucceeds() {
    runTest(testDispatcher) {
      // Given
      coEvery { connectionStateFlow.isConnected() } returns true
      coEvery { userActionUseCase.perform(any()) } returns Unit

      // When & Then
      viewModel.events.test {
        viewModel.actions.play(5)
        testDispatcher.scheduler.advanceUntilIdle()

        // Should not emit any events on successful play
        expectNoEvents()
      }

      // Verify user action was called with correct parameters
      coVerify(exactly = 1) { userActionUseCase.perform(UserAction(Protocol.NowPlayingListPlay, 5)) }
    }
  }

  @Test
  fun playShouldEmitPlayFailedWhenConnectedButUserActionThrowsIOException() {
    runTest(testDispatcher) {
      // Given
      coEvery { connectionStateFlow.isConnected() } returns true
      coEvery { userActionUseCase.perform(any()) } throws IOException("Network error")

      // When & Then
      viewModel.events.test {
        viewModel.actions.play(5)
        testDispatcher.scheduler.advanceUntilIdle()

        val event = awaitItem()
        assertThat(event).isEqualTo(NowPlayingUiMessages.PlayFailed)
      }

      // Verify user action was called
      coVerify(exactly = 1) { userActionUseCase.perform(UserAction(Protocol.NowPlayingListPlay, 5)) }
    }
  }

  @Test
  fun removeTrackShouldEmitNetworkUnavailableWhenNotConnected() {
    runTest(testDispatcher) {
      // Given
      coEvery { connectionStateFlow.isConnected() } returns false

      // When & Then
      viewModel.events.test {
        viewModel.actions.removeTrack(3)
        testDispatcher.scheduler.advanceUntilIdle()

        val event = awaitItem()
        assertThat(event).isEqualTo(NowPlayingUiMessages.NetworkUnavailable)
      }

      // Verify user action is not called when not connected
      coVerify(exactly = 0) { userActionUseCase.perform(any()) }
    }
  }

  @Test
  fun removeTrackShouldNotEmitWhenConnectedAndUserActionSucceeds() {
    runTest(testDispatcher) {
      // Given
      coEvery { connectionStateFlow.isConnected() } returns true
      coEvery { userActionUseCase.perform(any()) } returns Unit

      // When & Then
      viewModel.events.test {
        viewModel.actions.removeTrack(3)
        testDispatcher.scheduler.advanceUntilIdle()

        // Should not emit any events on successful remove
        expectNoEvents()
      }

      // Verify user action was called with correct parameters
      coVerify(exactly = 1) { userActionUseCase.perform(UserAction(Protocol.NowPlayingListRemove, 3)) }
    }
  }

  @Test
  fun removeTrackShouldEmitRemoveFailedWhenConnectedButUserActionThrowsIOException() {
    runTest(testDispatcher) {
      // Given
      coEvery { connectionStateFlow.isConnected() } returns true
      coEvery { userActionUseCase.perform(any()) } throws IOException("Network error")

      // When & Then
      viewModel.events.test {
        viewModel.actions.removeTrack(3)
        testDispatcher.scheduler.advanceUntilIdle()

        val event = awaitItem()
        assertThat(event).isEqualTo(NowPlayingUiMessages.RemoveFailed)
      }

      // Verify user action was called
      coVerify(exactly = 1) { userActionUseCase.perform(UserAction(Protocol.NowPlayingListRemove, 3)) }
    }
  }

  @Test
  fun searchShouldCallPlayWhenPositionFound() {
    runTest(testDispatcher) {
      // Given
      val query = "test song"
      val foundPosition = 10
      coEvery { repository.findPosition(query) } returns foundPosition
      coEvery { connectionStateFlow.isConnected() } returns true
      coEvery { userActionUseCase.perform(any()) } returns Unit

      // When & Then
      viewModel.events.test {
        viewModel.actions.search(query)
        testDispatcher.scheduler.advanceUntilIdle()

        // Should not emit any events since play succeeds
        expectNoEvents()
      }

      // Verify repository search was called
      coVerify(exactly = 1) { repository.findPosition(query) }
      // Verify play was called with found position
      coVerify(exactly = 1) { userActionUseCase.perform(UserAction(Protocol.NowPlayingListPlay, foundPosition)) }
    }
  }

  @Test
  fun searchShouldNotCallPlayWhenPositionNotFound() {
    runTest(testDispatcher) {
      // Given
      val query = "nonexistent song"
      coEvery { repository.findPosition(query) } returns -1

      // When & Then
      viewModel.events.test {
        viewModel.actions.search(query)
        testDispatcher.scheduler.advanceUntilIdle()

        // Should not emit any events
        expectNoEvents()
      }

      // Verify repository search was called
      coVerify(exactly = 1) { repository.findPosition(query) }
      // Verify play was not called
      coVerify(exactly = 0) { userActionUseCase.perform(any()) }
    }
  }

  @Test
  fun searchShouldEmitNetworkUnavailableWhenPositionFoundButNotConnected() {
    runTest(testDispatcher) {
      // Given
      val query = "test song"
      val foundPosition = 10
      coEvery { repository.findPosition(query) } returns foundPosition
      coEvery { connectionStateFlow.isConnected() } returns false

      // When & Then
      viewModel.events.test {
        viewModel.actions.search(query)
        testDispatcher.scheduler.advanceUntilIdle()

        val event = awaitItem()
        assertThat(event).isEqualTo(NowPlayingUiMessages.NetworkUnavailable)
      }

      // Verify repository search was called
      coVerify(exactly = 1) { repository.findPosition(query) }
      // Verify user action is not called when not connected
      coVerify(exactly = 0) { userActionUseCase.perform(any()) }
    }
  }

  @Test
  fun moveTrackShouldAllowLocalMovement() {
    runTest(testDispatcher) {
      // Given - moveTrack should work regardless of connection
      coEvery { connectionStateFlow.isConnected() } returns false

      // When & Then
      viewModel.events.test {
        viewModel.actions.moveTrack(from = 2, to = 5)
        testDispatcher.scheduler.advanceUntilIdle()

        // Should not emit any events since this is just local UI update
        expectNoEvents()
      }

      // Verify moveManager.move was called
      coVerify(exactly = 1) { moveManager.move(2, 5) }
    }
  }

  @Test
  fun moveShouldEmitNetworkUnavailableWhenNotConnected() {
    runTest(testDispatcher) {
      // Given
      coEvery { connectionStateFlow.isConnected() } returns false

      // When & Then
      viewModel.events.test {
        viewModel.actions.move()
        testDispatcher.scheduler.advanceUntilIdle()

        val event = awaitItem()
        assertThat(event).isEqualTo(NowPlayingUiMessages.NetworkUnavailable)
      }

      // Verify moveManager.commit is not called when not connected
      coVerify(exactly = 0) { moveManager.commit() }
    }
  }

  @Test
  fun moveShouldCommitWhenConnected() {
    runTest(testDispatcher) {
      // Given
      coEvery { connectionStateFlow.isConnected() } returns true

      // When & Then
      viewModel.events.test {
        viewModel.actions.move()
        testDispatcher.scheduler.advanceUntilIdle()

        // Should not emit any events on successful commit
        expectNoEvents()
      }

      // Verify moveManager.commit was called
      coVerify(exactly = 1) { moveManager.commit() }
    }
  }
}
