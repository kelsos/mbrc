package com.kelsos.mbrc.features.playlists

import androidx.paging.PagingData
import androidx.test.ext.junit.runners.AndroidJUnit4
import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import com.kelsos.mbrc.common.state.ConnectionStateFlow
import com.kelsos.mbrc.networking.protocol.Protocol
import com.kelsos.mbrc.networking.protocol.UserAction
import com.kelsos.mbrc.networking.protocol.UserActionUseCase
import com.kelsos.mbrc.utils.testDispatcher
import com.kelsos.mbrc.utils.testDispatcherModule
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import java.io.IOException
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

@RunWith(AndroidJUnit4::class)
class PlaylistViewModelTest : KoinTest {
  private val testModule =
    module {
      single<PlaylistRepository> { mockk(relaxed = true) }
      single<UserActionUseCase> { mockk(relaxed = true) }
      single<ConnectionStateFlow> { mockk(relaxed = true) }
      singleOf(::PlaylistViewModel)
    }

  private val viewModel: PlaylistViewModel by inject()
  private val playlistRepository: PlaylistRepository by inject()
  private val userActionUseCase: UserActionUseCase by inject()
  private val connectionStateFlow: ConnectionStateFlow by inject()

  @Before
  fun setUp() {
    startKoin {
      modules(listOf(testModule, testDispatcherModule))
    }

    // Setup default mocks
    every { playlistRepository.getAll() } returns flowOf(PagingData.empty())
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
        assertThat(event).isEqualTo(PlaylistUiMessages.NetworkUnavailable)
      }

      // Verify repository is not called when not connected
      coVerify(exactly = 0) { playlistRepository.getRemote() }
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
      coVerify(exactly = 0) { playlistRepository.getRemote() }
    }
  }

  @Test
  fun reloadShouldEmitRefreshSuccessWhenConnectedAndRepositorySucceedsAndShowUserMessageTrue() {
    runTest(testDispatcher) {
      // Given
      coEvery { connectionStateFlow.isConnected() } returns true
      coEvery { playlistRepository.getRemote() } returns Unit

      // When & Then
      viewModel.events.test {
        viewModel.actions.reload(showUserMessage = true)
        testDispatcher.scheduler.advanceUntilIdle()

        val event = awaitItem()
        assertThat(event).isEqualTo(PlaylistUiMessages.RefreshSuccess)
      }

      // Verify repository was called
      coVerify(exactly = 1) { playlistRepository.getRemote() }
    }
  }

  @Test
  fun reloadShouldNotEmitWhenConnectedAndRepositorySucceedsAndShowUserMessageFalse() {
    runTest(testDispatcher) {
      // Given
      coEvery { connectionStateFlow.isConnected() } returns true
      coEvery { playlistRepository.getRemote() } returns Unit

      // When & Then
      viewModel.events.test {
        viewModel.actions.reload(showUserMessage = false)
        testDispatcher.scheduler.advanceUntilIdle()

        // Should not emit any events on success when showUserMessage is false
        expectNoEvents()
      }

      // Verify repository was called
      coVerify(exactly = 1) { playlistRepository.getRemote() }
    }
  }

  @Test
  fun reloadShouldEmitRefreshFailedWhenConnectedButRepositoryThrowsAndShowUserMessageTrue() {
    runTest(testDispatcher) {
      // Given
      coEvery { connectionStateFlow.isConnected() } returns true
      coEvery { playlistRepository.getRemote() } throws IOException("Network error")

      // When & Then
      viewModel.events.test {
        viewModel.actions.reload(showUserMessage = true)
        testDispatcher.scheduler.advanceUntilIdle()

        val event = awaitItem()
        assertThat(event).isEqualTo(PlaylistUiMessages.RefreshFailed)
      }

      // Verify repository was called
      coVerify(exactly = 1) { playlistRepository.getRemote() }
    }
  }

  @Test
  fun reloadShouldNotEmitWhenConnectedButRepositoryThrowsIOExceptionAndShowUserMessageFalse() {
    runTest(testDispatcher) {
      // Given
      coEvery { connectionStateFlow.isConnected() } returns true
      coEvery { playlistRepository.getRemote() } throws IOException("Network error")

      // When & Then
      viewModel.events.test {
        viewModel.actions.reload(showUserMessage = false)
        testDispatcher.scheduler.advanceUntilIdle()

        // Should not emit any events on failure when showUserMessage is false
        expectNoEvents()
      }

      // Verify repository was called
      coVerify(exactly = 1) { playlistRepository.getRemote() }
    }
  }

  @Test
  fun reloadWithoutParameterShouldDefaultToShowUserMessageTrue() {
    runTest(testDispatcher) {
      // Given
      coEvery { connectionStateFlow.isConnected() } returns true
      coEvery { playlistRepository.getRemote() } returns Unit

      // When & Then
      viewModel.events.test {
        viewModel.actions.reload()
        testDispatcher.scheduler.advanceUntilIdle()

        val event = awaitItem()
        assertThat(event).isEqualTo(PlaylistUiMessages.RefreshSuccess)
      }

      // Verify repository was called
      coVerify(exactly = 1) { playlistRepository.getRemote() }
    }
  }

  @Test
  fun playShouldEmitNetworkUnavailableWhenNotConnected() {
    runTest(testDispatcher) {
      // Given
      val playlistPath = "playlist/test"
      coEvery { connectionStateFlow.isConnected() } returns false

      // When & Then
      viewModel.events.test {
        viewModel.actions.play(playlistPath)
        testDispatcher.scheduler.advanceUntilIdle()

        val event = awaitItem()
        assertThat(event).isEqualTo(PlaylistUiMessages.NetworkUnavailable)
      }

      // Verify user action is not called when not connected
      coVerify(exactly = 0) { userActionUseCase.perform(any()) }
    }
  }

  @Test
  fun playShouldNotEmitWhenConnectedAndUserActionSucceeds() {
    runTest(testDispatcher) {
      // Given
      val playlistPath = "playlist/test"
      coEvery { connectionStateFlow.isConnected() } returns true
      coEvery { userActionUseCase.perform(any()) } returns Unit

      // When & Then
      viewModel.events.test {
        viewModel.actions.play(playlistPath)
        testDispatcher.scheduler.advanceUntilIdle()

        // Should not emit any events on successful play
        expectNoEvents()
      }

      // Verify user action was called with correct parameters
      coVerify(exactly = 1) {
        userActionUseCase.perform(UserAction.create(Protocol.PlaylistPlay, playlistPath))
      }
    }
  }

  @Test
  fun playShouldEmitPlayFailedWhenConnectedButUserActionThrowsIOException() {
    runTest(testDispatcher) {
      // Given
      val playlistPath = "playlist/test"
      coEvery { connectionStateFlow.isConnected() } returns true
      coEvery { userActionUseCase.perform(any()) } throws IOException("Network error")

      // When & Then
      viewModel.events.test {
        viewModel.actions.play(playlistPath)
        testDispatcher.scheduler.advanceUntilIdle()

        val event = awaitItem()
        assertThat(event).isEqualTo(PlaylistUiMessages.PlayFailed)
      }

      // Verify user action was called
      coVerify(exactly = 1) {
        userActionUseCase.perform(UserAction.create(Protocol.PlaylistPlay, playlistPath))
      }
    }
  }

  @Test
  fun playlistsShouldReturnRepositoryPagingData() {
    // Given
    val pagingData = PagingData.empty<Playlist>()
    every { playlistRepository.getAll() } returns flowOf(pagingData)

    // Then
    assertThat(viewModel.playlists).isNotNull()
    // Note: PagingData testing requires more setup, this verifies the flow is accessible
  }

  @Test
  fun multipleReloadCallsWithDifferentParametersShouldBehaveCorrectly() {
    runTest(testDispatcher) {
      // Given
      coEvery { connectionStateFlow.isConnected() } returns true
      coEvery { playlistRepository.getRemote() } returns Unit

      // When & Then
      viewModel.events.test {
        viewModel.actions.reload(showUserMessage = true)
        viewModel.actions.reload(showUserMessage = false)
        testDispatcher.scheduler.advanceUntilIdle()

        // Only the first call should emit an event
        val event = awaitItem()
        assertThat(event).isEqualTo(PlaylistUiMessages.RefreshSuccess)
        expectNoEvents()
      }

      // Verify repository was called twice
      coVerify(exactly = 2) { playlistRepository.getRemote() }
    }
  }

  @Test
  fun networkCheckIsPerformedAtStartOfOperation() {
    runTest(testDispatcher) {
      // Given - connection starts as true, then becomes false
      coEvery { connectionStateFlow.isConnected() } returns true andThen false
      coEvery { playlistRepository.getRemote() } returns Unit

      // When & Then - First call should succeed, second should fail
      viewModel.events.test {
        viewModel.actions.reload(showUserMessage = true) // Should succeed (first call)
        testDispatcher.scheduler.advanceUntilIdle()

        val firstEvent = awaitItem()
        assertThat(firstEvent).isEqualTo(PlaylistUiMessages.RefreshSuccess)

        viewModel.actions.reload(showUserMessage = true) // Should fail (second call)
        testDispatcher.scheduler.advanceUntilIdle()

        val secondEvent = awaitItem()
        assertThat(secondEvent).isEqualTo(PlaylistUiMessages.NetworkUnavailable)
      }
    }
  }
}
