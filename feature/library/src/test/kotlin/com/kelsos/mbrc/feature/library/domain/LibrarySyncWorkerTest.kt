package com.kelsos.mbrc.feature.library.domain

import android.app.NotificationManager
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.work.ListenableWorker
import androidx.work.WorkerParameters
import androidx.work.testing.TestListenableWorkerBuilder
import androidx.work.workDataOf
import com.google.common.truth.Truth.assertThat
import com.kelsos.mbrc.core.common.utilities.AppError
import com.kelsos.mbrc.core.common.utilities.Outcome
import com.kelsos.mbrc.feature.library.data.LibraryStats
import com.kelsos.mbrc.feature.library.ui.LibraryMediaType
import io.mockk.coEvery
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Covers how the worker turns a sync outcome into a WorkManager result.
 *
 * The mapping is not one to one: a [AppError.NoOp] failure is reported as *success*, because an
 * automatic sync that declined to run is not something the user should see as a failed sync.
 */
@RunWith(AndroidJUnit4::class)
class LibrarySyncWorkerTest {

  private val context: Context = ApplicationProvider.getApplicationContext()
  private lateinit var syncUseCase: LibrarySyncUseCase
  private lateinit var notificationManager: NotificationManager

  private val stats = LibraryStats(
    genres = 1,
    artists = 2,
    albums = 3,
    tracks = 4,
    playlists = 5,
    covers = 6
  )

  @Before
  fun setUp() {
    syncUseCase = mockk()
    notificationManager = mockk(relaxed = true)
  }

  private fun worker(auto: Boolean = false): LibrarySyncWorker =
    TestListenableWorkerBuilder<LibrarySyncWorker>(
      context = context,
      inputData = workDataOf(LibrarySyncWorker.AUTO to auto)
    ).setWorkerFactory(
      object : androidx.work.WorkerFactory() {
        override fun createWorker(
          appContext: Context,
          workerClassName: String,
          workerParameters: WorkerParameters
        ): ListenableWorker =
          LibrarySyncWorker(appContext, workerParameters, syncUseCase, notificationManager)
      }
    ).build()

  @Test
  fun `a completed sync succeeds and carries the library counts`() = runTest {
    coEvery { syncUseCase.sync(any(), any()) } returns Outcome.Success(stats)

    val result = worker().doWork()

    assertThat(result).isInstanceOf(ListenableWorker.Result.Success::class.java)
    val data = (result as ListenableWorker.Result.Success).outputData
    assertThat(data.getLong("genres", -1)).isEqualTo(1)
    assertThat(data.getLong("tracks", -1)).isEqualTo(4)
    assertThat(data.getLong("covers", -1)).isEqualTo(6)
  }

  @Test
  fun `a sync that declined to run is reported as success, not failure`() = runTest {
    coEvery { syncUseCase.sync(any(), any()) } returns Outcome.Failure(AppError.NoOp)

    val result = worker(auto = true).doWork()

    assertThat(result).isInstanceOf(ListenableWorker.Result.Success::class.java)
  }

  @Test
  fun `a failure with a message fails and carries the message`() = runTest {
    coEvery { syncUseCase.sync(any(), any()) } returns
      Outcome.Failure(AppError.Message("the plugin went away"))

    val result = worker().doWork()

    assertThat(result).isInstanceOf(ListenableWorker.Result.Failure::class.java)
    val data = (result as ListenableWorker.Result.Failure).outputData
    assertThat(data.getString("error")).isEqualTo("the plugin went away")
  }

  @Test
  fun `any other failure fails with an unknown error`() = runTest {
    coEvery { syncUseCase.sync(any(), any()) } returns Outcome.Failure(AppError.NotConnected)

    val result = worker().doWork()

    assertThat(result).isInstanceOf(ListenableWorker.Result.Failure::class.java)
    val data = (result as ListenableWorker.Result.Failure).outputData
    assertThat(data.getString("error")).isEqualTo("Unknown error")
  }

  @Test
  fun `the auto flag reaches the sync use case`() = runTest {
    val auto = slot<Boolean>()
    coEvery { syncUseCase.sync(capture(auto), any()) } returns Outcome.Success(stats)

    worker(auto = true).doWork()

    assertThat(auto.captured).isTrue()
  }

  @Test
  fun `sync progress is published to the notification`() = runTest {
    coEvery { syncUseCase.sync(any(), any()) } coAnswers {
      secondArg<SyncProgress>().invoke(LibraryMediaType.Artists, 3, 10)
      Outcome.Success(stats)
    }

    worker().doWork()

    io.mockk.verify { notificationManager.notify(LibrarySyncWorker.NOTIFICATION_ID, any()) }
  }
}
