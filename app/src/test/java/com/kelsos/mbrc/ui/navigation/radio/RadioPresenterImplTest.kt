package com.kelsos.mbrc.ui.navigation.radio

import android.os.Looper
import com.kelsos.mbrc.data.RadioStation
import com.kelsos.mbrc.di.modules.AppDispatchers
import com.kelsos.mbrc.helper.QueueHandler
import com.kelsos.mbrc.helper.QueueResult
import com.kelsos.mbrc.repository.RadioRepository
import com.kelsos.mbrc.ui.navigation.radio.RadioActivity.Presenter
import com.raizlabs.android.dbflow.list.FlowCursorList
import io.mockk.*
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertThrows
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain
import org.junit.rules.TestRule
import toothpick.config.Module
import toothpick.testing.ToothPickRule

class RadioPresenterImplTest {

  private val toothpickRule: ToothPickRule = ToothPickRule(this, Presenter::class.java)
    .setRootRegistryPackage("com.kelsos.mbrc")

  @Rule
  @JvmField
  val chain: TestRule = RuleChain.outerRule(toothpickRule)
  private val radioView: RadioView = mockk()
  private val radioRepository: RadioRepository = mockk()
  private val queue: QueueHandler = mockk()
  private val result: FlowCursorList<RadioStation> = mockk()
  private val testDispatcher = StandardTestDispatcher()

  private lateinit var presenter: RadioPresenter

  @Before
  fun setUp() {
    toothpickRule.scope.installModules(RadioModule(), TestModule())
    presenter = toothpickRule.getInstance(RadioPresenter::class.java)
    every { radioView.showLoading() } just Runs
    every { radioView.hideLoading() } just Runs
    every { radioView.radioPlayFailed() } just Runs
    every { radioView.radioPlaySuccessful() } just Runs
    every { radioView.update(any()) } just Runs
    every { radioView.error(any()) } just Runs
    coEvery { queue.queuePath(any()) } coAnswers { QueueResult(true, 1) }

    mockkStatic(Looper::class)

    val looper = mockk<Looper> {
      every { thread } returns Thread.currentThread()
    }

    every { Looper.getMainLooper() } returns looper
  }

  @After
  fun tearDown() {
    unmockkAll()
  }

  @Test
  fun loadRadios_cacheEmpty_ViewAttached() = runTest(testDispatcher) {
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
  fun loadRadios_cacheNotEmpty_viewAttached() = runTest(testDispatcher) {
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
  fun loadRadios_loadError_ViewAttached() = runTest(testDispatcher) {
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
  fun loadRadios_loadError_ViewNotAttached() = runTest {
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
  fun loadRadios_refresh_ViewAttached() = runTest(testDispatcher) {
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
  fun loadRadios_refresh_ViewNotAttached() = runTest {
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
  fun loadRadios_refreshError_ViewAttached() = runTest(testDispatcher) {
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
  fun loadRadios_refreshError_ViewNotAttached() = runTest {
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
  fun playRadio_successful_ViewAttached() = runTest(testDispatcher) {
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
  fun playRadio_successful_ViewNotAttached() = runTest {
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
  fun playRadio_failure_ViewAttached() = runTest(testDispatcher) {
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
  fun playRadio_failure_ViewNotAttached() = runTest {
    val path = "http://fake.rad"
    coEvery { queue.queuePath(path) } returns QueueResult(false, 1)

    assertThrows(UninitializedPropertyAccessException::class.java) {
      presenter.play(path)
    }

    coVerify(exactly = 0) { queue.queuePath(path) }
    verify(exactly = 0) { radioView.radioPlayFailed() }
    verify(exactly = 0) { radioView.radioPlaySuccessful() }
  }

  inner class TestModule : Module() {
    init {
      bind(AppDispatchers::class.java).toInstance(
        AppDispatchers(
          testDispatcher,
          testDispatcher,
          testDispatcher
        )
      )
      bind(RadioView::class.java).toInstance(radioView)
      bind(RadioRepository::class.java).toInstance(radioRepository)
      bind(QueueHandler::class.java).toInstance(queue)
    }
  }
}
