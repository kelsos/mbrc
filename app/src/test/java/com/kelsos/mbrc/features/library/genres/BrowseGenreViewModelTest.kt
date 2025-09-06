package com.kelsos.mbrc.features.library.genres

import androidx.paging.PagingData
import androidx.test.ext.junit.runners.AndroidJUnit4
import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import com.kelsos.mbrc.common.state.ConnectionStateFlow
import com.kelsos.mbrc.features.library.LibrarySearchModel
import com.kelsos.mbrc.features.library.LibrarySyncUseCase
import com.kelsos.mbrc.features.queue.Queue
import com.kelsos.mbrc.features.queue.QueueHandler
import com.kelsos.mbrc.features.queue.QueueResult
import com.kelsos.mbrc.features.settings.SettingsManager
import com.kelsos.mbrc.features.settings.TrackAction
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

@RunWith(AndroidJUnit4::class)
class BrowseGenreViewModelTest : KoinTest {
  private val searchTermFlow = MutableStateFlow("")

  private val testModule =
    module {
      single<GenreRepository> { mockk(relaxed = true) }
      single<LibrarySyncUseCase> { mockk(relaxed = true) }
      single<QueueHandler> { mockk(relaxed = true) }
      single<ConnectionStateFlow> { mockk(relaxed = true) }
      single<SettingsManager> { mockk(relaxed = true) }
      single<LibrarySearchModel> { mockk(relaxed = true) }
      singleOf(::BrowseGenreViewModel)
    }

  private val viewModel: BrowseGenreViewModel by inject()
  private val repository: GenreRepository by inject()
  private val librarySyncUseCase: LibrarySyncUseCase by inject()
  private val queueHandler: QueueHandler by inject()
  private val connectionStateFlow: ConnectionStateFlow by inject()
  private val settingsManager: SettingsManager by inject()
  private val searchModel: LibrarySearchModel by inject()

  @Before
  fun setUp() {
    startKoin {
      modules(listOf(testModule, testDispatcherModule))
    }

    // Setup default mocks
    every { searchModel.term } returns searchTermFlow
    every { repository.getAll() } returns flowOf(PagingData.empty())
    every { repository.search(any()) } returns flowOf(PagingData.empty())
    every { settingsManager.libraryTrackDefaultActionFlow } returns flowOf(TrackAction.PlayNow)
    coEvery { connectionStateFlow.isConnected() } returns true
  }

  @After
  fun tearDown() {
    stopKoin()
  }

  @Test
  fun queueShouldEmitNetworkUnavailableWhenNotConnected() {
    runTest(testDispatcher) {
      // Given
      coEvery { connectionStateFlow.isConnected() } returns false
      val genre = Genre(id = 1, genre = "Rock")

      // When & Then
      viewModel.events.test {
        viewModel.queue(Queue.Next, genre)
        testDispatcher.scheduler.advanceUntilIdle()

        val event = awaitItem()
        assertThat(event).isEqualTo(GenreUiMessage.NetworkUnavailable)
      }

      // Verify queue handler is not called when not connected
      coVerify(exactly = 0) { queueHandler.queueGenre(any(), any()) }
    }
  }

  @Test
  fun queueShouldEmitQueueSuccessWhenConnectedAndQueueSucceeds() {
    runTest(testDispatcher) {
      // Given
      coEvery { connectionStateFlow.isConnected() } returns true
      val genre = Genre(id = 1, genre = "Rock")
      val queueResult = QueueResult(success = true, tracks = 25)
      coEvery { queueHandler.queueGenre(Queue.Next, "Rock") } returns queueResult

      // When & Then
      viewModel.events.test {
        viewModel.queue(Queue.Next, genre)
        testDispatcher.scheduler.advanceUntilIdle()

        val event = awaitItem()
        assertThat(event).isEqualTo(GenreUiMessage.QueueSuccess(25))
      }

      // Verify queue handler was called
      coVerify(exactly = 1) { queueHandler.queueGenre(Queue.Next, "Rock") }
    }
  }

  @Test
  fun queueShouldEmitQueueFailedWhenConnectedButQueueFails() {
    runTest(testDispatcher) {
      // Given
      coEvery { connectionStateFlow.isConnected() } returns true
      val genre = Genre(id = 1, genre = "Rock")
      val queueResult = QueueResult(success = false, tracks = 0)
      coEvery { queueHandler.queueGenre(Queue.Next, "Rock") } returns queueResult

      // When & Then
      viewModel.events.test {
        viewModel.queue(Queue.Next, genre)
        testDispatcher.scheduler.advanceUntilIdle()

        val event = awaitItem()
        assertThat(event).isEqualTo(GenreUiMessage.QueueFailed)
      }

      // Verify queue handler was called
      coVerify(exactly = 1) { queueHandler.queueGenre(Queue.Next, "Rock") }
    }
  }

  @Test
  fun queueShouldEmitOpenArtistsWhenQueueIsDefault() {
    runTest(testDispatcher) {
      // Given
      val genre = Genre(id = 1, genre = "Rock")

      // When & Then
      viewModel.events.test {
        viewModel.queue(Queue.Default, genre)
        testDispatcher.scheduler.advanceUntilIdle()

        val event = awaitItem()
        assertThat(event).isEqualTo(GenreUiMessage.OpenArtists(genre))
      }

      // Verify queue handler is not called for default action
      coVerify(exactly = 0) { queueHandler.queueGenre(any(), any()) }
    }
  }

  @Test
  fun syncShouldEmitNetworkUnavailableWhenNotConnected() {
    runTest(testDispatcher) {
      // Given
      coEvery { connectionStateFlow.isConnected() } returns false

      // When & Then
      viewModel.events.test {
        viewModel.sync()
        testDispatcher.scheduler.advanceUntilIdle()

        val event = awaitItem()
        assertThat(event).isEqualTo(GenreUiMessage.NetworkUnavailable)
      }

      // Verify sync use case is not called when not connected
      coVerify(exactly = 0) { librarySyncUseCase.sync() }
    }
  }

  @Test
  fun syncShouldCallSyncUseCaseWhenConnected() {
    runTest(testDispatcher) {
      // Given
      coEvery { connectionStateFlow.isConnected() } returns true

      // When & Then
      viewModel.events.test {
        viewModel.sync()
        testDispatcher.scheduler.advanceUntilIdle()

        // Should not emit any events for successful sync
        expectNoEvents()
      }

      // Verify sync use case was called
      coVerify(exactly = 1) { librarySyncUseCase.sync() }
    }
  }

  @Test
  fun networkCheckIsPerformedAtStartOfQueueOperation() {
    runTest(testDispatcher) {
      // Given - connection starts as true, then becomes false
      coEvery { connectionStateFlow.isConnected() } returns true andThen false
      val genre = Genre(id = 1, genre = "Rock")
      val queueResult = QueueResult(success = true, tracks = 12)
      coEvery { queueHandler.queueGenre(any(), any()) } returns queueResult

      // When & Then - First call should succeed, second should fail
      viewModel.events.test {
        viewModel.queue(Queue.Next, genre) // Should succeed (first call)
        testDispatcher.scheduler.advanceUntilIdle()

        val firstEvent = awaitItem()
        assertThat(firstEvent).isEqualTo(GenreUiMessage.QueueSuccess(12))

        viewModel.queue(Queue.Next, genre) // Should fail (second call)
        testDispatcher.scheduler.advanceUntilIdle()

        val secondEvent = awaitItem()
        assertThat(secondEvent).isEqualTo(GenreUiMessage.NetworkUnavailable)
      }

      // Verify queue handler was only called once (when connected)
      coVerify(exactly = 1) { queueHandler.queueGenre(any(), any()) }
    }
  }

  @Test
  fun networkCheckIsPerformedAtStartOfSyncOperation() {
    runTest(testDispatcher) {
      // Given - connection starts as true, then becomes false
      coEvery { connectionStateFlow.isConnected() } returns true andThen false

      // When & Then - First call should succeed, second should fail
      viewModel.events.test {
        viewModel.sync() // Should succeed (first call)
        testDispatcher.scheduler.advanceUntilIdle()

        // No events expected for successful sync
        expectNoEvents()

        viewModel.sync() // Should fail (second call)
        testDispatcher.scheduler.advanceUntilIdle()

        val event = awaitItem()
        assertThat(event).isEqualTo(GenreUiMessage.NetworkUnavailable)
      }

      // Verify sync use case was only called once (when connected)
      coVerify(exactly = 1) { librarySyncUseCase.sync() }
    }
  }
}
