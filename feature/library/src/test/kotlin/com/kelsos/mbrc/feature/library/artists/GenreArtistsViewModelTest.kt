package com.kelsos.mbrc.feature.library.artists

import androidx.paging.PagingData
import androidx.test.ext.junit.runners.AndroidJUnit4
import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import com.kelsos.mbrc.core.common.settings.ArtistSortField
import com.kelsos.mbrc.core.common.settings.ArtistSortPreference
import com.kelsos.mbrc.core.common.settings.LibrarySettings
import com.kelsos.mbrc.core.common.settings.SortOrder
import com.kelsos.mbrc.core.common.settings.TrackAction
import com.kelsos.mbrc.core.common.state.ConnectionStateFlow
import com.kelsos.mbrc.core.common.test.testDispatcher
import com.kelsos.mbrc.core.common.test.testDispatcherModule
import com.kelsos.mbrc.core.common.utilities.AppError
import com.kelsos.mbrc.core.common.utilities.Outcome
import com.kelsos.mbrc.core.data.library.artist.Artist
import com.kelsos.mbrc.core.data.library.artist.ArtistRepository
import com.kelsos.mbrc.core.queue.Queue
import com.kelsos.mbrc.feature.library.queue.QueueHandler
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
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
class GenreArtistsViewModelTest : KoinTest {
  private val testModule =
    module {
      single<ArtistRepository> { mockk(relaxed = true) }
      single<QueueHandler> { mockk(relaxed = true) }
      single<ConnectionStateFlow> { mockk(relaxed = true) }
      single<LibrarySettings> { mockk(relaxed = true) }
      singleOf(::GenreArtistsViewModel)
    }

  private val viewModel: GenreArtistsViewModel by inject()
  private val repository: ArtistRepository by inject()
  private val queueHandler: QueueHandler by inject()
  private val connectionStateFlow: ConnectionStateFlow by inject()
  private val librarySettings: LibrarySettings by inject()

  @Before
  fun setUp() {
    startKoin {
      modules(listOf(testModule, testDispatcherModule))
    }

    // Setup default mocks
    every { repository.getArtistByGenre(any(), any()) } returns flowOf(PagingData.empty())
    every { librarySettings.libraryTrackDefaultActionFlow } returns flowOf(TrackAction.PlayNow)
    every { librarySettings.artistSortPreferenceFlow } returns flowOf(
      ArtistSortPreference(ArtistSortField.NAME, SortOrder.ASC)
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
      val artist =
        Artist(
          id = 1,
          artist = "Test Artist"
        )

      // When & Then
      viewModel.events.test {
        viewModel.queue(Queue.Next, artist)
        testDispatcher.scheduler.advanceUntilIdle()

        val event = awaitItem()
        assertThat(event).isEqualTo(ArtistUiMessage.NetworkUnavailable)
      }

      // Verify queue handler is not called when not connected
      coVerify(exactly = 0) { queueHandler.queueArtist(any<Queue>(), any<String>()) }
    }
  }

  @Test
  fun queueShouldEmitQueueSuccessWhenConnectedAndQueueSucceeds() {
    runTest(testDispatcher) {
      // Given
      coEvery { connectionStateFlow.isConnected } returns true
      val artist =
        Artist(
          id = 1,
          artist = "Test Artist"
        )
      val queueResult = Outcome.Success(15)
      coEvery { queueHandler.queueArtist(Queue.Next, "Test Artist") } returns queueResult

      // When & Then
      viewModel.events.test {
        viewModel.queue(Queue.Next, artist)
        testDispatcher.scheduler.advanceUntilIdle()

        val event = awaitItem()
        assertThat(event).isEqualTo(ArtistUiMessage.QueueSuccess(15))
      }

      // Verify queue handler was called
      coVerify(exactly = 1) { queueHandler.queueArtist(Queue.Next, "Test Artist") }
    }
  }

  @Test
  fun queueShouldEmitQueueFailedWhenConnectedButQueueFails() {
    runTest(testDispatcher) {
      // Given
      coEvery { connectionStateFlow.isConnected } returns true
      val artist =
        Artist(
          id = 1,
          artist = "Test Artist"
        )
      val queueResult = Outcome.Failure(AppError.OperationFailed)
      coEvery { queueHandler.queueArtist(Queue.Next, "Test Artist") } returns queueResult

      // When & Then
      viewModel.events.test {
        viewModel.queue(Queue.Next, artist)
        testDispatcher.scheduler.advanceUntilIdle()

        val event = awaitItem()
        assertThat(event).isEqualTo(ArtistUiMessage.QueueFailed)
      }

      // Verify queue handler was called
      coVerify(exactly = 1) { queueHandler.queueArtist(Queue.Next, "Test Artist") }
    }
  }

  @Test
  fun queueShouldEmitOpenArtistAlbumsWhenQueueIsDefault() {
    runTest(testDispatcher) {
      // Given
      val artist =
        Artist(
          id = 1,
          artist = "Test Artist"
        )

      // When & Then
      viewModel.events.test {
        viewModel.queue(Queue.Default, artist)
        testDispatcher.scheduler.advanceUntilIdle()

        val event = awaitItem()
        assertThat(event).isEqualTo(ArtistUiMessage.OpenArtistAlbums(artist))
      }

      // Verify queue handler is not called for default action
      coVerify(exactly = 0) { queueHandler.queueArtist(any<Queue>(), any<String>()) }
    }
  }

  @Test
  fun networkCheckIsPerformedAtStartOfQueueOperation() {
    runTest(testDispatcher) {
      // Given - connection starts as true, then becomes false
      coEvery { connectionStateFlow.isConnected } returns true andThen false
      val artist =
        Artist(
          id = 1,
          artist = "Test Artist"
        )
      val queueResult = Outcome.Success(15)
      coEvery { queueHandler.queueArtist(any<Queue>(), any<String>()) } returns queueResult

      // When & Then - First call should succeed, second should fail
      viewModel.events.test {
        viewModel.queue(Queue.Next, artist) // Should succeed (first call)
        testDispatcher.scheduler.advanceUntilIdle()

        val firstEvent = awaitItem()
        assertThat(firstEvent).isEqualTo(ArtistUiMessage.QueueSuccess(15))

        viewModel.queue(Queue.Next, artist) // Should fail (second call)
        testDispatcher.scheduler.advanceUntilIdle()

        val secondEvent = awaitItem()
        assertThat(secondEvent).isEqualTo(ArtistUiMessage.NetworkUnavailable)
      }

      // Verify queue handler was only called once (when connected)
      coVerify(exactly = 1) { queueHandler.queueArtist(any<Queue>(), any<String>()) }
    }
  }

  @Test
  fun updateSortPreferenceShouldCallLibrarySettings() {
    runTest(testDispatcher) {
      // Given
      val preference = ArtistSortPreference(ArtistSortField.NAME, SortOrder.DESC)

      // When
      viewModel.updateSortPreference(preference)
      testDispatcher.scheduler.advanceUntilIdle()

      // Then
      coVerify(exactly = 1) { librarySettings.setArtistSortPreference(preference) }
    }
  }
}
