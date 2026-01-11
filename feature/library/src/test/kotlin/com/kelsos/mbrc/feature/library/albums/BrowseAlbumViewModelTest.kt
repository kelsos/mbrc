package com.kelsos.mbrc.feature.library.albums

import androidx.paging.PagingData
import androidx.test.ext.junit.runners.AndroidJUnit4
import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import com.kelsos.mbrc.core.common.settings.AlbumSortField
import com.kelsos.mbrc.core.common.settings.AlbumSortPreference
import com.kelsos.mbrc.core.common.settings.LibrarySettings
import com.kelsos.mbrc.core.common.settings.SortOrder
import com.kelsos.mbrc.core.common.settings.TrackAction
import com.kelsos.mbrc.core.common.state.ConnectionStateFlow
import com.kelsos.mbrc.core.common.test.testDispatcher
import com.kelsos.mbrc.core.common.test.testDispatcherModule
import com.kelsos.mbrc.core.common.utilities.AppError
import com.kelsos.mbrc.core.common.utilities.Outcome
import com.kelsos.mbrc.core.data.library.album.Album
import com.kelsos.mbrc.core.data.library.album.AlbumRepository
import com.kelsos.mbrc.core.queue.Queue
import com.kelsos.mbrc.feature.library.LibrarySearchModel
import com.kelsos.mbrc.feature.library.domain.LibrarySyncUseCase
import com.kelsos.mbrc.feature.library.queue.QueueHandler
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
class BrowseAlbumViewModelTest : KoinTest {
  private val searchTermFlow = MutableStateFlow("")

  private val testModule =
    module {
      single<AlbumRepository> { mockk(relaxed = true) }
      single<LibrarySyncUseCase> { mockk(relaxed = true) }
      single<QueueHandler> { mockk(relaxed = true) }
      single<ConnectionStateFlow> { mockk(relaxed = true) }
      single<LibrarySettings> { mockk(relaxed = true) }
      single<LibrarySearchModel> { mockk(relaxed = true) }
      singleOf(::BrowseAlbumViewModel)
    }

  private val viewModel: BrowseAlbumViewModel by inject()
  private val repository: AlbumRepository by inject()
  private val librarySyncUseCase: LibrarySyncUseCase by inject()
  private val queueHandler: QueueHandler by inject()
  private val connectionStateFlow: ConnectionStateFlow by inject()
  private val librarySettings: LibrarySettings by inject()
  private val searchModel: LibrarySearchModel by inject()

  @Before
  fun setUp() {
    startKoin {
      modules(listOf(testModule, testDispatcherModule))
    }

    // Setup default mocks
    every { searchModel.term } returns searchTermFlow
    every { repository.getAll(any(), any()) } returns flowOf(PagingData.empty())
    every { repository.search(any(), any(), any()) } returns flowOf(PagingData.empty())
    every { librarySettings.libraryTrackDefaultActionFlow } returns flowOf(TrackAction.PlayNow)
    every { librarySettings.albumSortPreferenceFlow } returns flowOf(
      AlbumSortPreference(AlbumSortField.NAME, SortOrder.ASC)
    )
    coEvery { connectionStateFlow.isConnected } returns true
  }

  @After
  fun tearDown() {
    stopKoin()
  }

  @Test
  fun queueShouldEmitNetworkUnavailableWhenNotConnected() {
    runTest(testDispatcher) {
      // Given
      coEvery { connectionStateFlow.isConnected } returns false
      val album = Album(id = 1, album = "Test Album", artist = "Test Artist", cover = null)

      // When & Then
      viewModel.events.test {
        viewModel.queue(Queue.Next, album)
        testDispatcher.scheduler.advanceUntilIdle()

        val event = awaitItem()
        assertThat(event).isEqualTo(AlbumUiMessage.NetworkUnavailable)
      }

      // Verify queue handler is not called when not connected
      coVerify(exactly = 0) { queueHandler.queueAlbum(any(), any(), any()) }
    }
  }

  @Test
  fun queueShouldEmitQueueSuccessWhenConnectedAndQueueSucceeds() {
    runTest(testDispatcher) {
      // Given
      coEvery { connectionStateFlow.isConnected } returns true
      val album = Album(id = 1, album = "Test Album", artist = "Test Artist", cover = null)
      val queueResult = Outcome.Success(10)
      coEvery { queueHandler.queueAlbum(Queue.Next, "Test Album", "Test Artist") } returns
        queueResult

      // When & Then
      viewModel.events.test {
        viewModel.queue(Queue.Next, album)
        testDispatcher.scheduler.advanceUntilIdle()

        val event = awaitItem()
        assertThat(event).isEqualTo(AlbumUiMessage.QueueSuccess(10))
      }

      // Verify queue handler was called
      coVerify(exactly = 1) { queueHandler.queueAlbum(Queue.Next, "Test Album", "Test Artist") }
    }
  }

  @Test
  fun queueShouldEmitQueueFailedWhenConnectedButQueueFails() {
    runTest(testDispatcher) {
      // Given
      coEvery { connectionStateFlow.isConnected } returns true
      val album = Album(id = 1, album = "Test Album", artist = "Test Artist", cover = null)
      val queueResult = Outcome.Failure(AppError.OperationFailed)
      coEvery { queueHandler.queueAlbum(Queue.Next, "Test Album", "Test Artist") } returns
        queueResult

      // When & Then
      viewModel.events.test {
        viewModel.queue(Queue.Next, album)
        testDispatcher.scheduler.advanceUntilIdle()

        val event = awaitItem()
        assertThat(event).isEqualTo(AlbumUiMessage.QueueFailed)
      }

      // Verify queue handler was called
      coVerify(exactly = 1) { queueHandler.queueAlbum(Queue.Next, "Test Album", "Test Artist") }
    }
  }

  @Test
  fun queueShouldEmitOpenAlbumTracksWhenQueueIsDefault() {
    runTest(testDispatcher) {
      // Given
      val album = Album(id = 1, album = "Test Album", artist = "Test Artist", cover = null)

      // When & Then
      viewModel.events.test {
        viewModel.queue(Queue.Default, album)
        testDispatcher.scheduler.advanceUntilIdle()

        val event = awaitItem()
        assertThat(event).isEqualTo(AlbumUiMessage.OpenAlbumTracks(album))
      }

      // Verify queue handler is not called for default action
      coVerify(exactly = 0) { queueHandler.queueAlbum(any(), any(), any()) }
    }
  }

  @Test
  fun syncShouldEmitNetworkUnavailableWhenNotConnected() {
    runTest(testDispatcher) {
      // Given
      coEvery { connectionStateFlow.isConnected } returns false

      // When & Then
      viewModel.events.test {
        viewModel.sync()
        testDispatcher.scheduler.advanceUntilIdle()

        val event = awaitItem()
        assertThat(event).isEqualTo(AlbumUiMessage.NetworkUnavailable)
      }

      // Verify sync use case is not called when not connected
      coVerify(exactly = 0) { librarySyncUseCase.sync() }
    }
  }

  @Test
  fun syncShouldCallSyncUseCaseWhenConnected() {
    runTest(testDispatcher) {
      // Given
      coEvery { connectionStateFlow.isConnected } returns true

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
      coEvery { connectionStateFlow.isConnected } returns true andThen false
      val album = Album(id = 1, album = "Test Album", artist = "Test Artist", cover = null)
      val queueResult = Outcome.Success(5)
      coEvery { queueHandler.queueAlbum(any(), any(), any()) } returns queueResult

      // When & Then - First call should succeed, second should fail
      viewModel.events.test {
        viewModel.queue(Queue.Next, album) // Should succeed (first call)
        testDispatcher.scheduler.advanceUntilIdle()

        val firstEvent = awaitItem()
        assertThat(firstEvent).isEqualTo(AlbumUiMessage.QueueSuccess(5))

        viewModel.queue(Queue.Next, album) // Should fail (second call)
        testDispatcher.scheduler.advanceUntilIdle()

        val secondEvent = awaitItem()
        assertThat(secondEvent).isEqualTo(AlbumUiMessage.NetworkUnavailable)
      }

      // Verify queue handler was only called once (when connected)
      coVerify(exactly = 1) { queueHandler.queueAlbum(any(), any(), any()) }
    }
  }

  @Test
  fun networkCheckIsPerformedAtStartOfSyncOperation() {
    runTest(testDispatcher) {
      // Given - connection starts as true, then becomes false
      coEvery { connectionStateFlow.isConnected } returns true andThen false

      // When & Then - First call should succeed, second should fail
      viewModel.events.test {
        viewModel.sync() // Should succeed (first call)
        testDispatcher.scheduler.advanceUntilIdle()

        // No events expected for successful sync
        expectNoEvents()

        viewModel.sync() // Should fail (second call)
        testDispatcher.scheduler.advanceUntilIdle()

        val event = awaitItem()
        assertThat(event).isEqualTo(AlbumUiMessage.NetworkUnavailable)
      }

      // Verify sync use case was only called once (when connected)
      coVerify(exactly = 1) { librarySyncUseCase.sync() }
    }
  }

  @Test
  fun updateSortPreferenceShouldCallLibrarySettings() {
    runTest(testDispatcher) {
      // Given
      val preference = AlbumSortPreference(AlbumSortField.YEAR, SortOrder.DESC)

      // When
      viewModel.updateSortPreference(preference)
      testDispatcher.scheduler.advanceUntilIdle()

      // Then
      coVerify(exactly = 1) { librarySettings.setAlbumSortPreference(preference) }
    }
  }
}
