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
import io.mockk.Runs
import io.mockk.coEvery
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.advanceTimeBy
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
      val onCompleteListener = setupOnCompleteListener()

      mockCacheState(true)
      mockSuccessfulRepositoryResponse()

      sync.setOnCompleteListener(onCompleteListener)
      sync.sync(true)
      advanceTimeBy(TASK_DELAY)
      assertThat(sync.isRunning()).isTrue()
      advanceTimeBy(TASK_DELAY)
      assertThat(sync.isRunning()).isTrue()
      advanceTimeBy(5 * TASK_DELAY)

      verify(exactly = 1) { onCompleteListener.onSuccess(any()) }
      verify(exactly = 1) { onCompleteListener.onTermination() }
      verify(exactly = 0) { onCompleteListener.onFailure(any()) }

      assertThat(sync.isRunning()).isFalse()
    }
  }

  @Test
  fun nonEmptyLibraryAutoSync() {
    runTest(testDispatcher) {
      val onCompleteListener = setupOnCompleteListener()

      mockCacheState(false)
      mockSuccessfulRepositoryResponse()

      sync.setOnCompleteListener(onCompleteListener)
      sync.sync(true)

      advanceTimeBy(TASK_DELAY)

      verify(exactly = 0) { onCompleteListener.onSuccess(any()) }
      verify(exactly = 1) { onCompleteListener.onTermination() }
      verify(exactly = 0) { onCompleteListener.onFailure(any()) }

      assertThat(sync.isRunning()).isFalse()
    }
  }

  @Test
  fun nonEmptyLibraryManualSyncTwiceConsecutiveCalled() {
    runTest(testDispatcher) {
      val onCompleteListener = setupOnCompleteListener()

      mockCacheState(false)
      mockSuccessfulRepositoryResponse()

      sync.setOnCompleteListener(onCompleteListener)
      sync.sync()
      sync.sync()

      advanceTimeBy(TASK_DELAY)
      assertThat(sync.isRunning()).isTrue()
      advanceTimeBy(TASK_DELAY)
      assertThat(sync.isRunning()).isTrue()
      advanceTimeBy(5 * TASK_DELAY)

      verify(exactly = 1) { onCompleteListener.onSuccess(any()) }
      verify(exactly = 1) { onCompleteListener.onTermination() }
      verify(exactly = 0) { onCompleteListener.onFailure(any()) }

      assertThat(sync.isRunning()).isFalse()
    }
  }

  @Test
  fun nonEmptyLibraryManualSyncAndSecondAfterCompletion() {
    runTest(testDispatcher) {
      var onCompleteListener = setupOnCompleteListener()

      mockCacheState(false)
      mockSuccessfulRepositoryResponse()

      sync.setOnCompleteListener(onCompleteListener)
      sync.sync()

      advanceTimeBy(TASK_DELAY)
      assertThat(sync.isRunning()).isTrue()
      advanceTimeBy(TASK_DELAY)
      assertThat(sync.isRunning()).isTrue()
      advanceTimeBy(5 * TASK_DELAY)

      verify(exactly = 1) { onCompleteListener.onSuccess(any()) }
      verify(exactly = 1) { onCompleteListener.onTermination() }
      verify(exactly = 0) { onCompleteListener.onFailure(any()) }

      assertThat(sync.isRunning()).isFalse()

      onCompleteListener = setupOnCompleteListener()
      sync.setOnCompleteListener(onCompleteListener)

      sync.sync()

      advanceTimeBy(TASK_DELAY)
      assertThat(sync.isRunning()).isTrue()
      advanceTimeBy(TASK_DELAY)
      assertThat(sync.isRunning()).isTrue()
      advanceTimeBy(5 * TASK_DELAY)

      verify(exactly = 1) { onCompleteListener.onSuccess(any()) }
      verify(exactly = 1) { onCompleteListener.onTermination() }
      verify(exactly = 0) { onCompleteListener.onFailure(any()) }

      assertThat(sync.isRunning()).isFalse()
    }
  }

  @Test
  fun nonEmptyLibraryManualSyncFailure() {
    runTest(testDispatcher) {
      val onCompleteListener = setupOnCompleteListener()

      mockCacheState(false)
      mockFailedRepositoryResponse()

      sync.setOnCompleteListener(onCompleteListener)
      sync.sync()
      sync.sync()

      advanceTimeBy(TASK_DELAY / 2)
      assertThat(sync.isRunning()).isTrue()
      advanceTimeBy(5 * TASK_DELAY)

      verify(exactly = 0) { onCompleteListener.onSuccess(any()) }
      verify(exactly = 1) { onCompleteListener.onTermination() }
      verify(exactly = 1) { onCompleteListener.onFailure(ofType(Exception::class)) }

      assertThat(sync.isRunning()).isFalse()
    }
  }

  private fun setupOnCompleteListener(): LibrarySyncUseCase.OnCompleteListener {
    val onCompleteListener = mockk<LibrarySyncUseCase.OnCompleteListener>()
    every { onCompleteListener.onTermination() } just Runs
    every { onCompleteListener.onSuccess(any()) } just Runs
    every { onCompleteListener.onFailure(any()) } just Runs
    return onCompleteListener
  }

  @Test
  fun syncWithoutCompletionListener() {
    runTest(testDispatcher) {
      mockCacheState(false)
      mockSuccessfulRepositoryResponse()

      sync.sync()

      advanceTimeBy(TASK_DELAY)
      assertThat(sync.isRunning()).isTrue()
      advanceTimeBy(TASK_DELAY)
      assertThat(sync.isRunning()).isTrue()
      advanceTimeBy(5 * TASK_DELAY)

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
    coEvery { genreRepository.getRemote() } coAnswers { wait() }
    coEvery { artistRepository.getRemote() } coAnswers { wait() }
    coEvery { albumRepository.getRemote() } coAnswers { wait() }
    coEvery { trackRepository.getRemote() } coAnswers { wait() }
    coEvery { playlistRepository.getRemote() } coAnswers { wait() }
  }

  private fun mockFailedRepositoryResponse() {
    coEvery { genreRepository.getRemote() } coAnswers { wait() }
    coEvery { artistRepository.getRemote() } throws SocketTimeoutException()
    coEvery { albumRepository.getRemote() } coAnswers { wait() }
    coEvery { trackRepository.getRemote() } coAnswers { wait() }
    coEvery { playlistRepository.getRemote() } coAnswers { wait() }
  }

  companion object {
    const val TASK_DELAY = 400L
  }
}
