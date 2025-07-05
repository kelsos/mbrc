package com.kelsos.mbrc.features.library.artists

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
import com.kelsos.mbrc.features.settings.BasicSettingsHelper
import com.kelsos.mbrc.features.settings.SettingsManager
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
class BrowseArtistViewModelTest : KoinTest {
  private val searchTermFlow = MutableStateFlow("")
  private val shouldDisplayOnlyArtistsFlow = MutableStateFlow(false)

  private val testModule =
    module {
      single<ArtistRepository> { mockk(relaxed = true) }
      single<LibrarySyncUseCase> { mockk(relaxed = true) }
      single<QueueHandler> { mockk(relaxed = true) }
      single<ConnectionStateFlow> { mockk(relaxed = true) }
      single<BasicSettingsHelper> { mockk(relaxed = true) }
      single<LibrarySearchModel> { mockk(relaxed = true) }
      single<SettingsManager> { mockk(relaxed = true) }
      singleOf(::BrowseArtistViewModel)
    }

  private val viewModel: BrowseArtistViewModel by inject()
  private val repository: ArtistRepository by inject()
  private val librarySyncUseCase: LibrarySyncUseCase by inject()
  private val queueHandler: QueueHandler by inject()
  private val connectionStateFlow: ConnectionStateFlow by inject()
  private val settingsHelper: BasicSettingsHelper by inject()
  private val searchModel: LibrarySearchModel by inject()
  private val settingsManager: SettingsManager by inject()

  @Before
  fun setUp() {
    startKoin {
      modules(listOf(testModule, testDispatcherModule))
    }

    // Setup default mocks
    every { searchModel.term } returns searchTermFlow
    every { settingsManager.shouldDisplayOnlyArtists } returns shouldDisplayOnlyArtistsFlow
    every { repository.getAll() } returns flowOf(PagingData.empty())
    every { repository.getAlbumArtistsOnly() } returns flowOf(PagingData.empty())
    every { repository.search(any()) } returns flowOf(PagingData.empty())
    every { settingsHelper.defaultAction } returns "Now"
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
      val artist = Artist(id = 1, artist = "Test Artist")

      // When & Then
      viewModel.events.test {
        viewModel.queue(Queue.Next, artist)
        testDispatcher.scheduler.advanceUntilIdle()

        val event = awaitItem()
        assertThat(event).isEqualTo(ArtistUiMessage.NetworkUnavailable)
      }

      // Verify queue handler is not called when not connected
      coVerify(exactly = 0) { queueHandler.queueArtist(any(), any()) }
    }
  }

  @Test
  fun queueShouldEmitQueueSuccessWhenConnectedAndQueueSucceeds() {
    runTest(testDispatcher) {
      // Given
      coEvery { connectionStateFlow.isConnected() } returns true
      val artist = Artist(id = 1, artist = "Test Artist")
      val queueResult = QueueResult(success = true, tracks = 15)
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
      coEvery { connectionStateFlow.isConnected() } returns true
      val artist = Artist(id = 1, artist = "Test Artist")
      val queueResult = QueueResult(success = false, tracks = 0)
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
      val artist = Artist(id = 1, artist = "Test Artist")

      // When & Then
      viewModel.events.test {
        viewModel.queue(Queue.Default, artist)
        testDispatcher.scheduler.advanceUntilIdle()

        val event = awaitItem()
        assertThat(event).isEqualTo(ArtistUiMessage.OpenArtistAlbums(artist))
      }

      // Verify queue handler is not called for default action
      coVerify(exactly = 0) { queueHandler.queueArtist(any(), any()) }
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
        assertThat(event).isEqualTo(ArtistUiMessage.NetworkUnavailable)
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
      val artist = Artist(id = 1, artist = "Test Artist")
      val queueResult = QueueResult(success = true, tracks = 8)
      coEvery { queueHandler.queueArtist(any(), any()) } returns queueResult

      // When & Then - First call should succeed, second should fail
      viewModel.events.test {
        viewModel.queue(Queue.Next, artist) // Should succeed (first call)
        testDispatcher.scheduler.advanceUntilIdle()

        val firstEvent = awaitItem()
        assertThat(firstEvent).isEqualTo(ArtistUiMessage.QueueSuccess(8))

        viewModel.queue(Queue.Next, artist) // Should fail (second call)
        testDispatcher.scheduler.advanceUntilIdle()

        val secondEvent = awaitItem()
        assertThat(secondEvent).isEqualTo(ArtistUiMessage.NetworkUnavailable)
      }

      // Verify queue handler was only called once (when connected)
      coVerify(exactly = 1) { queueHandler.queueArtist(any(), any()) }
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
        assertThat(event).isEqualTo(ArtistUiMessage.NetworkUnavailable)
      }

      // Verify sync use case was only called once (when connected)
      coVerify(exactly = 1) { librarySyncUseCase.sync() }
    }
  }
}
