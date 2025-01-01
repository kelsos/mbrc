package com.kelsos.mbrc.features.radio

import android.os.Looper
import com.kelsos.mbrc.common.utilities.AppCoroutineDispatchers
import com.kelsos.mbrc.features.queue.QueueHandler
import com.kelsos.mbrc.features.queue.QueueResult
import com.raizlabs.android.dbflow.list.FlowCursorList
import io.mockk.Runs
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkAll
import io.mockk.verify
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertThrows
import org.junit.Before
import org.junit.Test
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.core.module.dsl.bind
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.module
import org.koin.test.KoinTest
import org.koin.test.inject

class RadioPresenterImplTest : KoinTest {
  private val radioView: RadioView by inject()
  private val radioRepository: RadioRepository by inject()
  private val queue: QueueHandler by inject()
  private val result: FlowCursorList<RadioStation> = mockk()
  private val presenter: RadioPresenter by inject()
  private val testDispatcher = StandardTestDispatcher()

  private val testModule =
    module {
      single {
        AppCoroutineDispatchers(
          testDispatcher,
          testDispatcher,
          testDispatcher,
          testDispatcher,
        )
      }
      single { mockk<RadioView>() }
      single { mockk<RadioRepository>() }
      single { mockk<QueueHandler>() }
      singleOf(::RadioPresenterImpl) { bind<RadioPresenter>() }
    }

  @Before
  fun setUp() {
    startKoin {
      modules(listOf(testModule))
    }
    every { radioView.showLoading() } just Runs
    every { radioView.hideLoading() } just Runs
    every { radioView.radioPlayFailed() } just Runs
    every { radioView.radioPlaySuccessful() } just Runs
    every { radioView.update(any()) } just Runs
    every { radioView.error(any()) } just Runs
    coEvery { queue.queuePath(any()) } coAnswers { QueueResult(true, 1) }

    mockkStatic(Looper::class)

    val looper =
      mockk<Looper> {
        every { thread } returns Thread.currentThread()
      }

    every { Looper.getMainLooper() } returns looper
  }

  @After
  fun tearDown() {
    stopKoin()
    unmockkAll()
  }

  @Test
  fun loadRadios_cacheEmpty_ViewAttached() =
    runTest(testDispatcher) {
      val data = result
      coEvery { radioRepository.cacheIsEmpty() } returns true
      coEvery { radioRepository.getAndSaveRemote() } returns data

      presenter.attach(radioView)
      presenter.load()

      testDispatcher.scheduler.advanceUntilIdle()

      coVerify(exactly = 1) { radioRepository.getAndSaveRemote() }
      verify(exactly = 1) { radioView.update(result) }
      verify(exactly = 0) { radioView.error(any()) }
      verify(exactly = 1) { radioView.showLoading() }
      verify(exactly = 1) { radioView.hideLoading() }
    }

  @Test
  fun loadRadios_cacheNotEmpty_viewAttached() =
    runTest(testDispatcher) {
      coEvery { radioRepository.cacheIsEmpty() } returns false
      coEvery { radioRepository.getAndSaveRemote() } returns result
      presenter.attach(radioView)
      presenter.load()

      testDispatcher.scheduler.advanceUntilIdle()

      coVerify(exactly = 1) { radioRepository.getAndSaveRemote() }
      verify(exactly = 1) { radioView.update(result) }
      verify(exactly = 0) { radioView.error(any()) }
      verify(exactly = 1) { radioView.showLoading() }
      verify(exactly = 1) { radioView.hideLoading() }
    }

  @Test
  fun loadRadios_loadError_ViewAttached() =
    runTest(testDispatcher) {
      val exception = RuntimeException()
      coEvery { radioRepository.cacheIsEmpty() } returns false
      coEvery { radioRepository.getAndSaveRemote() } throws exception

      presenter.attach(radioView)
      presenter.load()

      testDispatcher.scheduler.advanceUntilIdle()

      coVerify(exactly = 1) { radioRepository.getAndSaveRemote() }
      verify(exactly = 0) { radioView.update(result) }
      verify(exactly = 1) { radioView.error(exception) }
      verify(exactly = 1) { radioView.showLoading() }
      verify(exactly = 1) { radioView.hideLoading() }
    }

  @Test
  fun loadRadios_loadError_ViewNotAttached() =
    runTest {
      val exception = RuntimeException()
      coEvery { radioRepository.cacheIsEmpty() } returns false

      assertThrows(UninitializedPropertyAccessException::class.java) {
        presenter.load()
      }

      coVerify(exactly = 0) { radioRepository.getAndSaveRemote() }
      verify(exactly = 0) { radioView.update(result) }
      verify(exactly = 0) { radioView.error(exception) }
      verify(exactly = 0) { radioView.showLoading() }
      verify(exactly = 0) { radioView.hideLoading() }
    }

  @Test
  fun loadRadios_refresh_ViewAttached() =
    runTest(testDispatcher) {
      val data = result
      coEvery { radioRepository.getAndSaveRemote() } returns data

      presenter.attach(radioView)
      presenter.refresh()

      testDispatcher.scheduler.advanceUntilIdle()

      coVerify(exactly = 1) { radioRepository.getAndSaveRemote() }
      verify(exactly = 1) { radioView.update(result) }
      verify(exactly = 0) { radioView.error(any()) }
      verify(exactly = 1) { radioView.showLoading() }
      verify(exactly = 1) { radioView.hideLoading() }
    }

  @Test
  fun loadRadios_refresh_ViewNotAttached() =
    runTest {
      val data = result
      coEvery { radioRepository.getAndSaveRemote() } returns data

      assertThrows(UninitializedPropertyAccessException::class.java) {
        presenter.refresh()
      }

      coVerify(exactly = 0) { radioRepository.getAndSaveRemote() }
      verify(exactly = 0) { radioView.update(result) }
      verify(exactly = 0) { radioView.error(any()) }
      verify(exactly = 0) { radioView.showLoading() }
      verify(exactly = 0) { radioView.hideLoading() }
    }

  @Test
  fun loadRadios_refreshError_ViewAttached() =
    runTest(testDispatcher) {
      val error = RuntimeException()
      coEvery { radioRepository.getAndSaveRemote() } throws error

      presenter.attach(radioView)
      presenter.refresh()

      testDispatcher.scheduler.advanceUntilIdle()

      coVerify(exactly = 1) { radioRepository.getAndSaveRemote() }
      verify(exactly = 0) { radioView.update(result) }
      verify(exactly = 1) { radioView.error(error) }
      verify(exactly = 1) { radioView.showLoading() }
      verify(exactly = 1) { radioView.hideLoading() }
    }

  @Test
  fun loadRadios_refreshError_ViewNotAttached() =
    runTest {
      val error = RuntimeException()
      coEvery { radioRepository.getAndSaveRemote() } throws error

      assertThrows(UninitializedPropertyAccessException::class.java) {
        presenter.refresh()
      }

      coVerify(exactly = 0) { radioRepository.getAndSaveRemote() }
      verify(exactly = 0) { radioView.update(result) }
      verify(exactly = 0) { radioView.error(error) }
      verify(exactly = 0) { radioView.showLoading() }
      verify(exactly = 0) { radioView.hideLoading() }
    }

  @Test
  fun playRadio_successful_ViewAttached() =
    runTest(testDispatcher) {
      val path = "http://fake.rad"
      coEvery { queue.queuePath(path) } returns QueueResult(true, 1)

      presenter.attach(radioView)
      presenter.play(path)

      testDispatcher.scheduler.advanceUntilIdle()

      coVerify(exactly = 1) { queue.queuePath(path) }
      verify(exactly = 0) { radioView.radioPlayFailed() }
      verify(exactly = 1) { radioView.radioPlaySuccessful() }
    }

  @Test
  fun playRadio_successful_ViewNotAttached() =
    runTest {
      val path = "http://fake.rad"
      coEvery { queue.queuePath(path) } returns QueueResult(true, 1)

      assertThrows(UninitializedPropertyAccessException::class.java) {
        presenter.play(path)
      }

      coVerify(exactly = 0) { queue.queuePath(path) }
      verify(exactly = 0) { radioView.radioPlayFailed() }
      verify(exactly = 0) { radioView.radioPlaySuccessful() }
    }

  @Test
  fun playRadio_failure_ViewAttached() =
    runTest(testDispatcher) {
      val path = "http://fake.rad"
      coEvery { queue.queuePath(path) } returns QueueResult(false, 1)

      presenter.attach(radioView)
      presenter.play(path)

      testDispatcher.scheduler.advanceUntilIdle()

      coVerify(exactly = 1) { queue.queuePath(path) }
      verify(exactly = 1) { radioView.radioPlayFailed() }
      verify(exactly = 0) { radioView.radioPlaySuccessful() }
    }

  @Test
  fun playRadio_failure_ViewNotAttached() =
    runTest {
      val path = "http://fake.rad"
      coEvery { queue.queuePath(path) } returns QueueResult(false, 1)

      assertThrows(UninitializedPropertyAccessException::class.java) {
        presenter.play(path)
      }

      coVerify(exactly = 0) { queue.queuePath(path) }
      verify(exactly = 0) { radioView.radioPlayFailed() }
      verify(exactly = 0) { radioView.radioPlaySuccessful() }
    }
}
