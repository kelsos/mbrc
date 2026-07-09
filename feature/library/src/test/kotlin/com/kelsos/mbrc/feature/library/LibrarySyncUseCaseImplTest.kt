@file:OptIn(ExperimentalCoroutinesApi::class)

package com.kelsos.mbrc.feature.library

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import com.kelsos.mbrc.core.common.test.parserModule
import com.kelsos.mbrc.core.common.test.testDispatcher
import com.kelsos.mbrc.core.common.test.testDispatcherModule
import com.kelsos.mbrc.core.common.utilities.AppError
import com.kelsos.mbrc.core.common.utilities.Outcome
import com.kelsos.mbrc.core.data.cacheIsEmpty
import com.kelsos.mbrc.core.data.library.album.AlbumRepository
import com.kelsos.mbrc.core.data.library.artist.ArtistRepository
import com.kelsos.mbrc.core.data.library.genre.GenreRepository
import com.kelsos.mbrc.core.data.library.track.TrackRepository
import com.kelsos.mbrc.core.data.playlist.PlaylistRepository
import com.kelsos.mbrc.feature.library.data.CoverCache
import com.kelsos.mbrc.feature.library.domain.LibraryDerivationUseCase
import com.kelsos.mbrc.feature.library.domain.LibrarySyncUseCase
import com.kelsos.mbrc.feature.library.domain.LibrarySyncUseCaseImpl
import com.kelsos.mbrc.feature.library.domain.SyncOutcome
import io.mockk.coEvery
import io.mockk.mockk
import java.net.SocketTimeoutException
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
import org.junit.runner.RunWith
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.core.module.dsl.bind
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.module
import org.koin.test.KoinTest
import org.koin.test.inject

@RunWith(AndroidJUnit4::class)
class LibrarySyncUseCaseImplTest : KoinTest {
  private val testModule =
    module {
      includes(testDispatcherModule, parserModule)

      singleOf(::LibrarySyncUseCaseImpl) { bind<LibrarySyncUseCase>() }
      single<GenreRepository> { mockk() }
      single<ArtistRepository> { mockk() }
      single<AlbumRepository> { mockk() }
      single<TrackRepository> { mockk() }
      single<PlaylistRepository> { mockk() }
      single<LibraryDerivationUseCase> { mockk() }
      single<CoverCache> { mockk(relaxed = true) }
    }

  private val genreRepository: GenreRepository by inject()
  private val artistRepository: ArtistRepository by inject()
  private val albumRepository: AlbumRepository by inject()
  private val trackRepository: TrackRepository by inject()
  private val playlistRepository: PlaylistRepository by inject()
  private val derivationUseCase: LibraryDerivationUseCase by inject()
  private val sync: LibrarySyncUseCase by inject()

  @Before
  fun setUp() {
    startKoin { modules(listOf(testModule)) }
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

      var syncResult: SyncOutcome? = null

      val job = launch { syncResult = sync.sync(true) }

      advanceTimeBy(TASK_DELAY)
      assertThat(sync.isRunning()).isTrue()
      advanceTimeBy(TASK_DELAY)
      assertThat(sync.isRunning()).isTrue()
      advanceTimeBy(5 * TASK_DELAY)

      job.join()

      assertThat(syncResult).isInstanceOf(Outcome.Success::class.java)
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

      assertThat(syncResult).isEqualTo(Outcome.Failure(AppError.NoOp))
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

      assertThat(secondSyncResult).isEqualTo(Outcome.Failure(AppError.NoOp))

      advanceTimeBy(TASK_DELAY)
      assertThat(sync.isRunning()).isTrue()
      advanceTimeBy(5 * TASK_DELAY)

      val syncResult = firstJob.await()

      assertThat(syncResult).isInstanceOf(Outcome.Success::class.java)
      assertThat(sync.isRunning()).isFalse()
    }
  }

  @Test
  fun nonEmptyLibraryManualSyncAndSecondAfterCompletion() {
    runTest(testDispatcher) {
      mockCacheState(false)
      mockSuccessfulRepositoryResponse()

      var syncResult: SyncOutcome? = null
      val firstJob = launch { syncResult = sync.sync() }

      advanceTimeBy(TASK_DELAY)
      assertThat(sync.isRunning()).isTrue()
      advanceTimeBy(TASK_DELAY)
      assertThat(sync.isRunning()).isTrue()
      advanceUntilIdle()

      firstJob.join()

      assertThat(syncResult).isInstanceOf(Outcome.Success::class.java)
      assertThat(sync.isRunning()).isFalse()

      val secondJob = launch { syncResult = sync.sync() }

      advanceTimeBy(TASK_DELAY)
      assertThat(sync.isRunning()).isTrue()
      advanceTimeBy(TASK_DELAY)
      assertThat(sync.isRunning()).isTrue()
      advanceUntilIdle()

      secondJob.join()

      assertThat(syncResult).isInstanceOf(Outcome.Success::class.java)
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
      assertThat(secondSyncResult).isEqualTo(Outcome.Failure(AppError.NoOp))

      advanceTimeBy(5 * TASK_DELAY)

      val syncResult = firstJob.await()

      assertThat(syncResult).isInstanceOf(Outcome.Failure::class.java)
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
    coEvery { albumRepository.coverCount() } returns cached
  }

  private suspend fun wait() {
    delay(TASK_DELAY)
    return
  }

  private fun mockSuccessfulRepositoryResponse() {
    // Genres, artists and albums are now derived from tracks, not fetched.
    coEvery { trackRepository.getRemote(any()) } coAnswers { wait() }
    coEvery { derivationUseCase.derive(any()) } coAnswers { wait() }
    coEvery { playlistRepository.getRemote(any()) } coAnswers { wait() }
  }

  private fun mockFailedRepositoryResponse() {
    coEvery { trackRepository.getRemote(any()) } coAnswers {
      wait()
      throw SocketTimeoutException()
    }
    coEvery { derivationUseCase.derive(any()) } coAnswers { wait() }
    coEvery { playlistRepository.getRemote(any()) } coAnswers { wait() }
  }

  companion object {
    const val TASK_DELAY = 400L
  }
}
