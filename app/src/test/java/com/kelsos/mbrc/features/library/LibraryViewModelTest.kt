package com.kelsos.mbrc.features.library

import androidx.test.ext.junit.runners.AndroidJUnit4
import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import com.kelsos.mbrc.common.state.ConnectionStateFlow
import com.kelsos.mbrc.features.settings.SettingsManager
import com.kelsos.mbrc.utils.testDispatcher
import com.kelsos.mbrc.utils.testDispatcherModule
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
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
import org.koin.test.get
import org.koin.test.inject

@RunWith(AndroidJUnit4::class)
class LibraryViewModelTest : KoinTest {
  private val testModule =
    module {
      single<LibrarySearchModel> { mockk(relaxed = true) }
      single<LibrarySyncWorkHandler> { mockk(relaxed = true) }
      single<LibrarySyncUseCase> { mockk(relaxed = true) }
      single<SettingsManager> { mockk(relaxed = true) }
      single<ConnectionStateFlow> { mockk(relaxed = true) }
      singleOf(::LibraryViewModel)
    }

  private val viewModel: LibraryViewModel by inject()
  private val librarySearchModel: LibrarySearchModel by inject()
  private val librarySyncWorkHandler: LibrarySyncWorkHandler by inject()
  private val librarySyncUseCase: LibrarySyncUseCase by inject()
  private val settingsManager: SettingsManager by inject()
  private val connectionStateFlow: ConnectionStateFlow by inject()

  @Before
  fun setUp() {
    startKoin { modules(listOf(testModule, testDispatcherModule)) }

    // Setup default mocks
    every { librarySyncWorkHandler.syncProgress() } returns
      flowOf(
        LibrarySyncProgress(LibraryMediaType.Genres, 0, 0, false)
      )
    every { librarySyncWorkHandler.syncResults() } returns flowOf(SyncResult.Noop)
    coEvery { connectionStateFlow.isConnected() } returns true
  }

  @After
  fun tearDown() {
    stopKoin()
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
        assertThat(event).isEqualTo(LibraryUiEvent.NetworkUnavailable)
      }

      // Verify sync handler is not called when not connected
      coVerify(exactly = 0) { librarySyncWorkHandler.sync(any()) }
    }
  }

  @Test
  fun syncShouldCallSyncHandlerWhenConnected() {
    runTest(testDispatcher) {
      // Given
      coEvery { connectionStateFlow.isConnected() } returns true

      // When & Then
      viewModel.events.test {
        viewModel.sync()
        testDispatcher.scheduler.advanceUntilIdle()

        // Should not emit any events since sync() doesn't emit success events
        expectNoEvents()
      }

      // Verify sync handler was called
      coVerify(exactly = 1) { librarySyncWorkHandler.sync(false) }
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

        expectNoEvents() // No events on success

        viewModel.sync() // Should fail (second call)
        testDispatcher.scheduler.advanceUntilIdle()

        val event = awaitItem()
        assertThat(event).isEqualTo(LibraryUiEvent.NetworkUnavailable)
      }

      // Verify sync handler was called only once (for successful call)
      coVerify(exactly = 1) { librarySyncWorkHandler.sync(false) }
    }
  }

  @Test
  fun searchShouldEmitSearchTerm() {
    runTest(testDispatcher) {
      // Given
      val searchTerm = "test artist"

      // When
      viewModel.search(searchTerm)
      testDispatcher.scheduler.advanceUntilIdle()

      // Then
      coVerify(exactly = 1) { librarySearchModel.term.emit(searchTerm) }
    }
  }

  @Test
  fun searchWithEmptyStringShouldEmitEmptyString() {
    runTest(testDispatcher) {
      // When
      viewModel.search()
      testDispatcher.scheduler.advanceUntilIdle()

      // Then
      coVerify(exactly = 1) { librarySearchModel.term.emit("") }
    }
  }

  @Test
  fun updateAlbumArtistOnlyShouldCallSettingsManager() {
    // Given
    val enabled = true

    // When
    viewModel.updateAlbumArtistOnly(enabled)

    // Then
    verify(exactly = 1) { settingsManager.setShouldDisplayOnlyAlbumArtist(enabled) }
  }

  @Test
  fun displayLibraryStatsShouldEmitLibraryStatsReady() {
    runTest(testDispatcher) {
      // Given
      val mockStats =
        LibraryStats(
          genres = 10,
          artists = 100,
          albums = 200,
          tracks = 1000,
          playlists = 50,
          covers = 150
        )
      coEvery { librarySyncUseCase.syncStats() } returns mockStats

      // When & Then
      viewModel.events.test {
        viewModel.displayLibraryStats()
        testDispatcher.scheduler.advanceUntilIdle()

        val event = awaitItem()
        assertThat(event).isInstanceOf(LibraryUiEvent.LibraryStatsReady::class.java)
        assertThat((event as LibraryUiEvent.LibraryStatsReady).stats).isEqualTo(mockStats)
      }

      // Verify sync use case was called
      coVerify(exactly = 1) { librarySyncUseCase.syncStats() }
    }
  }

  @Test
  fun progressShouldReturnSyncHandlerProgress() {
    // Given
    val mockProgress = LibrarySyncProgress(LibraryMediaType.Albums, 50, 100, true)
    every { librarySyncWorkHandler.syncProgress() } returns flowOf(mockProgress)

    // When
    val newViewModel =
      LibraryViewModel(
        searchModel = librarySearchModel,
        librarySyncWorkHandler = librarySyncWorkHandler,
        librarySyncUseCase = librarySyncUseCase,
        settingsManager = settingsManager,
        connectionStateFlow = connectionStateFlow
      )

    // Then
    assertThat(newViewModel.progress).isNotNull()
    // Note: Flow testing would require more setup, this verifies the flow is accessible
  }

  @Test
  fun syncResultsShouldReturnSyncHandlerResults() {
    // Given
    val mockResult =
      SyncResult.Success(
        LibraryStats(
          genres = 5,
          artists = 50,
          albums = 100,
          tracks = 500,
          playlists = 25,
          covers = 75
        )
      )
    every { librarySyncWorkHandler.syncResults() } returns flowOf(mockResult)

    // When
    val newViewModel =
      LibraryViewModel(
        searchModel = librarySearchModel,
        librarySyncWorkHandler = librarySyncWorkHandler,
        librarySyncUseCase = librarySyncUseCase,
        settingsManager = settingsManager,
        connectionStateFlow = connectionStateFlow
      )

    // Then
    assertThat(newViewModel.syncResults).isNotNull()
    // Note: Flow testing would require more setup, this verifies the flow is accessible
  }

  @Test
  fun multipleSyncCallsWithDifferentConnectionStatesShouldBehaveCorrectly() {
    runTest(testDispatcher) {
      // Given
      coEvery { connectionStateFlow.isConnected() } returns true andThen false andThen true

      // When & Then
      viewModel.events.test {
        viewModel.sync() // Should succeed (first call)
        viewModel.sync() // Should fail (second call)
        viewModel.sync() // Should succeed (third call)
        testDispatcher.scheduler.advanceUntilIdle()

        // Should only emit one NetworkUnavailable event (for the second call)
        val event = awaitItem()
        assertThat(event).isEqualTo(LibraryUiEvent.NetworkUnavailable)
        expectNoEvents()
      }

      // Verify sync handler was called twice (for successful calls)
      coVerify(exactly = 2) { librarySyncWorkHandler.sync(false) }
    }
  }

  @Test
  fun concurrentSyncCallsShouldHandleNetworkFailuresIndependently() {
    runTest(testDispatcher) {
      // Given
      coEvery { connectionStateFlow.isConnected() } returns false andThen true

      // When & Then
      viewModel.events.test {
        viewModel.sync() // Should fail
        viewModel.sync() // Should succeed
        testDispatcher.scheduler.advanceUntilIdle()

        // Should only emit one NetworkUnavailable event
        val event = awaitItem()
        assertThat(event).isEqualTo(LibraryUiEvent.NetworkUnavailable)
        expectNoEvents()
      }

      // Verify sync handler was called once (for successful call)
      coVerify(exactly = 1) { librarySyncWorkHandler.sync(false) }
    }
  }
}
