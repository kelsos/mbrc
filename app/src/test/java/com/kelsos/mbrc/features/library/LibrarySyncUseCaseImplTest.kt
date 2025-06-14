@file:OptIn(ExperimentalCoroutinesApi::class)

package com.kelsos.mbrc.features.library

import com.google.common.truth.Truth.assertThat
import com.kelsos.mbrc.common.data.cacheIsEmpty
import com.kelsos.mbrc.features.library.albums.AlbumRepository
import com.kelsos.mbrc.features.library.artists.ArtistRepository
import com.kelsos.mbrc.features.library.genres.GenreRepository
import com.kelsos.mbrc.features.library.tracks.TrackRepository
import com.kelsos.mbrc.features.playlists.PlaylistRepository
import com.kelsos.mbrc.utils.parserModule
import com.kelsos.mbrc.utils.testDispatcher
import com.kelsos.mbrc.utils.testDispatcherModule
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.koin.core.context.GlobalContext.startKoin
import org.koin.core.context.stopKoin
import org.koin.core.module.dsl.bind
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.module
import org.koin.test.KoinTest
import org.koin.test.inject
import java.net.SocketTimeoutException

class LibrarySyncUseCaseImplTest : KoinTest {
  private val genreRepository: GenreRepository by inject()
  private val artistRepository: ArtistRepository by inject()
  private val albumRepository: AlbumRepository by inject()
  private val trackRepository: TrackRepository by inject()
  private val playlistRepository: PlaylistRepository by inject()
  private val sync: LibrarySyncUseCase by inject()

  private val testModule =
    module {
      includes(testDispatcherModule, parserModule)

      singleOf(::LibrarySyncUseCaseImpl) { bind<LibrarySyncUseCase>() }
      single { mockk<GenreRepository>() }
      single { mockk<ArtistRepository>() }
      single { mockk<AlbumRepository>() }
      single { mockk<TrackRepository>() }
      single { mockk<PlaylistRepository>() }
      single { mockk<CoverCache>(relaxed = true) }
    }

  @Before
  fun setUp() {
    startKoin {
      modules(listOf(testModule))
    }
  }

  @After
  fun tearDown() {
    stopKoin()
  }

  @Test
  fun emptyLibraryAutoSync() {
    runTest(testDispatcher) {
      mockCacheState(true)
      mockSuccessfulRepositoryResponse()

      var syncResult: SyncResult? = null

      val job = launch { syncResult = sync.sync(true) }

      advanceTimeBy(TASK_DELAY)
      assertThat(sync.isRunning()).isTrue()
      advanceTimeBy(TASK_DELAY)
      assertThat(sync.isRunning()).isTrue()
      advanceTimeBy(5 * TASK_DELAY)

      job.join()

      assertThat(syncResult).isInstanceOf(SyncResult.Success::class.java)
      assertThat(sync.isRunning()).isFalse()
    }
  }

  @Test
  fun nonEmptyLibraryAutoSync() {
    runTest(testDispatcher) {
      mockCacheState(false)
      mockSuccessfulRepositoryResponse()

      val syncResult = sync.sync(true)

      advanceTimeBy(TASK_DELAY)

      assertThat(syncResult).isEqualTo(SyncResult.Noop)
      assertThat(sync.isRunning()).isFalse()
    }
  }

  @Test
  fun nonEmptyLibraryManualSyncTwiceConsecutiveCalled() {
    runTest(testDispatcher) {
      mockCacheState(false)
      mockSuccessfulRepositoryResponse()

      val firstJob = async { sync.sync() }

      advanceTimeBy(TASK_DELAY / 2)
      assertThat(sync.isRunning()).isTrue()

      val secondSyncResult = sync.sync()

      assertThat(secondSyncResult).isEqualTo(SyncResult.Noop)

      advanceTimeBy(TASK_DELAY)
      assertThat(sync.isRunning()).isTrue()
      advanceTimeBy(5 * TASK_DELAY)

      val syncResult = firstJob.await()

      assertThat(syncResult).isInstanceOf(SyncResult.Success::class.java)
      assertThat(sync.isRunning()).isFalse()
    }
  }

  @Test
  fun nonEmptyLibraryManualSyncAndSecondAfterCompletion() {
    runTest(testDispatcher) {
      mockCacheState(false)
      mockSuccessfulRepositoryResponse()

      var syncResult: SyncResult? = null
      val firstJob = launch { syncResult = sync.sync() }

      advanceTimeBy(TASK_DELAY)
      assertThat(sync.isRunning()).isTrue()
      advanceTimeBy(TASK_DELAY)
      assertThat(sync.isRunning()).isTrue()
      advanceUntilIdle()

      firstJob.join()

      assertThat(syncResult).isInstanceOf(SyncResult.Success::class.java)
      assertThat(sync.isRunning()).isFalse()

      val secondJob = launch { syncResult = sync.sync() }

      advanceTimeBy(TASK_DELAY)
      assertThat(sync.isRunning()).isTrue()
      advanceTimeBy(TASK_DELAY)
      assertThat(sync.isRunning()).isTrue()
      advanceUntilIdle()

      secondJob.join()

      assertThat(syncResult).isInstanceOf(SyncResult.Success::class.java)
      assertThat(sync.isRunning()).isFalse()
    }
  }

  @Test
  fun nonEmptyLibraryManualSyncFailure() {
    runTest(testDispatcher) {
      mockCacheState(false)
      mockFailedRepositoryResponse()

      val firstJob = async { sync.sync() }

      advanceTimeBy(TASK_DELAY / 2)
      assertThat(sync.isRunning()).isTrue()

      val secondSyncResult = sync.sync()
      assertThat(secondSyncResult).isEqualTo(SyncResult.Noop)

      advanceTimeBy(5 * TASK_DELAY)

      val syncResult = firstJob.await()

      assertThat(syncResult).isInstanceOf(SyncResult.Failed::class.java)
      assertThat(sync.isRunning()).isFalse()
    }
  }

  private fun mockCacheState(isEmpty: Boolean) {
    coEvery { genreRepository.cacheIsEmpty() } returns isEmpty
    coEvery { artistRepository.cacheIsEmpty() } returns isEmpty
    coEvery { albumRepository.cacheIsEmpty() } returns isEmpty
    coEvery { trackRepository.cacheIsEmpty() } returns isEmpty
    coEvery { playlistRepository.cacheIsEmpty() } returns isEmpty

    val cached = if (isEmpty) 0L else 500L

    coEvery { genreRepository.count() } returns cached
    coEvery { artistRepository.count() } returns cached
    coEvery { albumRepository.count() } returns cached
    coEvery { trackRepository.count() } returns cached
    coEvery { playlistRepository.count() } returns cached
  }

  private suspend fun wait() {
    delay(TASK_DELAY)
    return
  }

  private fun mockSuccessfulRepositoryResponse() {
    coEvery { genreRepository.getRemote(any()) } coAnswers { wait() }
    coEvery { artistRepository.getRemote(any()) } coAnswers { wait() }
    coEvery { albumRepository.getRemote(any()) } coAnswers { wait() }
    coEvery { trackRepository.getRemote(any()) } coAnswers { wait() }
    coEvery { playlistRepository.getRemote(any()) } coAnswers { wait() }
  }

  private fun mockFailedRepositoryResponse() {
    coEvery { genreRepository.getRemote(any()) } coAnswers { wait() }
    coEvery { artistRepository.getRemote(any()) } throws SocketTimeoutException()
    coEvery { albumRepository.getRemote(any()) } coAnswers { wait() }
    coEvery { trackRepository.getRemote(any()) } coAnswers { wait() }
    coEvery { playlistRepository.getRemote(any()) } coAnswers { wait() }
  }

  companion object {
    const val TASK_DELAY = 400L
  }
}
