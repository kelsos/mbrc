@file:OptIn(ExperimentalCoroutinesApi::class)

package com.kelsos.mbrc.ui.navigation.library

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import com.google.common.truth.Truth.assertThat
import com.kelsos.mbrc.di.modules.AppDispatchers
import com.kelsos.mbrc.events.bus.RxBus
import com.kelsos.mbrc.events.ui.LibraryRefreshCompleteEvent
import com.kelsos.mbrc.repository.*
import io.mockk.*
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import toothpick.Scope
import toothpick.Toothpick
import toothpick.config.Module
import toothpick.testing.ToothPickRule
import toothpick.testing.ToothPickTestModule
import java.net.SocketTimeoutException

class LibrarySyncInteractorImplTest {
  @Rule
  fun toothPickRule() = ToothPickRule(this)

  private val TEST_CASE_SCOPE: Class<*> = TestCase::class.java

  lateinit var genreRepository: GenreRepository
  lateinit var artistRepository: ArtistRepository
  lateinit var albumRepository: AlbumRepository
  lateinit var trackRepository: TrackRepository
  lateinit var playlistRepository: PlaylistRepository
  lateinit var bus: RxBus

  private lateinit var scope: Scope

  private val testDispatcher = StandardTestDispatcher()

  @Before
  fun setUp() {
    genreRepository = mockk()
    artistRepository = mockk()
    albumRepository = mockk()
    trackRepository = mockk()
    playlistRepository = mockk()
    bus = mockk()
    scope = Toothpick.openScope(TEST_CASE_SCOPE)
    scope.installModules(
      ToothPickTestModule(this),
      object : Module() {
        init {
          bind(AppDispatchers::class.java).toInstance(
            AppDispatchers(
              testDispatcher,
              testDispatcher,
              testDispatcher,
            ),
          )
          bind(GenreRepository::class.java).toInstance(genreRepository)
          bind(ArtistRepository::class.java).toInstance(artistRepository)
          bind(AlbumRepository::class.java).toInstance(albumRepository)
          bind(TrackRepository::class.java).toInstance(trackRepository)
          bind(PlaylistRepository::class.java).toInstance(playlistRepository)
          bind(RxBus::class.java).toInstance(bus)
          bind(LibrarySyncInteractor::class.java).to(LibrarySyncInteractorImpl::class.java).singletonInScope()
          bind(ObjectMapper::class.java).toInstance(ObjectMapper().registerKotlinModule())
          bind(CoverCache::class.java).toInstance(mockk(relaxed = true))
        }
      },
    )

    every { bus.post(any()) } just Runs
  }

  @After
  fun tearDown() {
    Toothpick.closeScope(TEST_CASE_SCOPE)
    Toothpick.reset()
  }

  @Test
  fun emptyLibraryAutoSync() =
    runTest(testDispatcher) {
      val onCompleteListener = setupOnCompleteListener()
      val sync = scope.getInstance(LibrarySyncInteractor::class.java)

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
      verify(exactly = 1) { bus.post(ofType(LibraryRefreshCompleteEvent::class)) }

      assertThat(sync.isRunning()).isFalse()
    }

  @Test
  fun nonEmptyLibraryAutoSync() =
    runTest(testDispatcher) {
      val onCompleteListener = setupOnCompleteListener()
      val sync = scope.getInstance(LibrarySyncInteractor::class.java)

      mockCacheState(false)
      mockSuccessfulRepositoryResponse()

      sync.setOnCompleteListener(onCompleteListener)
      sync.sync(true)

      advanceTimeBy(TASK_DELAY)

      verify(exactly = 0) { onCompleteListener.onSuccess(any()) }
      verify(exactly = 1) { onCompleteListener.onTermination() }
      verify(exactly = 0) { onCompleteListener.onFailure(any()) }
      verify(exactly = 0) { bus.post(ofType(LibraryRefreshCompleteEvent::class)) }

      assertThat(sync.isRunning()).isFalse()
    }

  @Test
  fun nonEmptyLibraryManualSyncTwiceConsecutiveCalled() =
    runTest(testDispatcher) {
      val onCompleteListener = setupOnCompleteListener()
      val sync = scope.getInstance(LibrarySyncInteractor::class.java)

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
      verify(exactly = 1) { bus.post(ofType(LibraryRefreshCompleteEvent::class)) }

      assertThat(sync.isRunning()).isFalse()
    }

  @Test
  fun nonEmptyLibraryManualSyncAndSecondAfterCompletion() =
    runTest(testDispatcher) {
      var onCompleteListener = setupOnCompleteListener()
      val sync = scope.getInstance(LibrarySyncInteractor::class.java)

      mockCacheState(false)
      mockSuccessfulRepositoryResponse()

      sync.setOnCompleteListener(onCompleteListener)
      sync.sync()

      advanceTimeBy(TASK_DELAY)
      assertThat(sync.isRunning())
      advanceTimeBy(TASK_DELAY)
      assertThat(sync.isRunning()).isTrue()
      advanceTimeBy(5 * TASK_DELAY)

      verify(exactly = 1) { onCompleteListener.onSuccess(any()) }
      verify(exactly = 1) { onCompleteListener.onTermination() }
      verify(exactly = 0) { onCompleteListener.onFailure(any()) }
      verify(exactly = 1) { bus.post(ofType(LibraryRefreshCompleteEvent::class)) }

      assertThat(sync.isRunning()).isFalse()

      clearMocks(onCompleteListener, bus)
      every { bus.post(any()) } just Runs

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
      verify(exactly = 1) { bus.post(ofType(LibraryRefreshCompleteEvent::class)) }

      assertThat(sync.isRunning()).isFalse()
    }

  @Test
  fun nonEmptyLibraryManualSyncFailure() =
    runTest(testDispatcher) {
      val onCompleteListener = setupOnCompleteListener()
      val sync = scope.getInstance(LibrarySyncInteractor::class.java)

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
      verify(exactly = 0) { bus.post(ofType(LibraryRefreshCompleteEvent::class)) }

      assertThat(sync.isRunning()).isFalse()
    }

  private fun setupOnCompleteListener(): LibrarySyncInteractor.OnCompleteListener {
    val onCompleteListener = mockk<LibrarySyncInteractor.OnCompleteListener>()
    every { onCompleteListener.onTermination() } just Runs
    every { onCompleteListener.onSuccess(any()) } just Runs
    every { onCompleteListener.onFailure(any()) } just Runs
    return onCompleteListener
  }

  @Test
  fun syncWithoutCompletionListener() =
    runTest(testDispatcher) {
      val sync = scope.getInstance(LibrarySyncInteractor::class.java)

      mockCacheState(false)
      mockSuccessfulRepositoryResponse()

      sync.sync()

      advanceTimeBy(TASK_DELAY)
      assertThat(sync.isRunning()).isTrue()
      advanceTimeBy(TASK_DELAY)
      assertThat(sync.isRunning()).isTrue()
      advanceTimeBy(5 * TASK_DELAY)

      assertThat(sync.isRunning()).isFalse()
      verify(exactly = 1) { bus.post(ofType(LibraryRefreshCompleteEvent::class)) }
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

  @javax.inject.Scope
  @Target(AnnotationTarget.TYPE)
  @Retention(AnnotationRetention.RUNTIME)
  annotation class TestCase

  companion object {
    const val TASK_DELAY = 400L
  }
}
