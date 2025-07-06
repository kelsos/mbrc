package com.kelsos.mbrc.features.library

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.work.CoroutineWorker
import androidx.work.Data
import androidx.work.ExistingWorkPolicy
import androidx.work.ForegroundInfo
import androidx.work.OneTimeWorkRequest
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkInfo
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.kelsos.mbrc.R
import com.kelsos.mbrc.features.library.LibrarySyncWorker.Companion.CATEGORY
import com.kelsos.mbrc.features.library.LibrarySyncWorker.Companion.CURRENT
import com.kelsos.mbrc.features.library.LibrarySyncWorker.Companion.TOTAL
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.map
import timber.log.Timber

data class LibrarySyncProgress(
  val category: LibraryMediaType,
  val current: Int,
  val total: Int,
  val running: Boolean
)

interface LibrarySyncWorkHandler {
  fun sync(auto: Boolean = false)

  fun syncResults(): Flow<SyncResult>

  fun syncProgress(): Flow<LibrarySyncProgress>
}

class LibrarySyncWorkHandlerImpl(private val workManager: WorkManager) : LibrarySyncWorkHandler {
  override fun sync(auto: Boolean) {
    val workRequest = LibrarySyncWorker.createWorkRequest(auto)
    workManager.enqueueUniqueWork(
      LibrarySyncWorker.SYNC_WORK_TAG,
      ExistingWorkPolicy.REPLACE,
      workRequest
    )
  }

  override fun syncResults(): Flow<SyncResult> {
    return workManager
      .getWorkInfosForUniqueWorkFlow(LibrarySyncWorker.SYNC_WORK_TAG)
      .map { workInfoList ->
        if (workInfoList.isEmpty()) return@map null
        val workInfo = workInfoList.first()
        when (workInfo.state) {
          WorkInfo.State.SUCCEEDED -> {
            val data = workInfo.outputData
            val keys = data.keyValueMap.keys

            if (keys.any { it in listOf("genres", "artists", "albums", "tracks", "playlists") }) {
              SyncResult.Success(data.toLibraryStats())
            } else {
              SyncResult.Noop
            }
          }
          WorkInfo.State.FAILED -> {
            val message = workInfo.outputData.getString("error") ?: "Unknown failure"
            SyncResult.Failed(message)
          }
          WorkInfo.State.CANCELLED -> SyncResult.Failed("Sync was cancelled")
          else -> null
        }
      }.filterNotNull()
  }

  override fun syncProgress(): Flow<LibrarySyncProgress> {
    return workManager.getWorkInfosForUniqueWorkFlow(
      LibrarySyncWorker.SYNC_WORK_TAG
    ).map { progress ->
      if (progress.isEmpty()) {
        return@map LibrarySyncProgress(LibraryMediaType.Genres, 0, 0, false)
      }
      val workInfo = progress.first()
      val workProgress = workInfo.progress
      return@map LibrarySyncProgress(
        LibraryMediaType.fromCode(workProgress.getInt(CATEGORY, LibraryMediaType.Genres.code)),
        workProgress.getInt(CURRENT, 0),
        workProgress.getInt(TOTAL, 0),
        workInfo.state === WorkInfo.State.RUNNING
      )
    }
  }
}

class LibrarySyncWorker(
  context: Context,
  params: WorkerParameters,
  private val librarySyncUseCase: LibrarySyncUseCase,
  private val notificationManager: NotificationManager
) : CoroutineWorker(context, params) {
  private fun createSyncNotificationChannel() {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return

    val channel =
      NotificationChannel(
        NOTIFICATION_CHANNEL_ID,
        applicationContext.getString(R.string.sync_notification__channel_name),
        NotificationManager.IMPORTANCE_DEFAULT
      )

    channel.apply {
      this.description =
        applicationContext.getString(R.string.sync_notification__channel_description)
      enableLights(false)
      enableVibration(false)
      setSound(null, null)
    }

    notificationManager.createNotificationChannel(channel)
  }

  private fun createForegroundInfo(): ForegroundInfo {
    val title = applicationContext.getString(R.string.notification__sync_title)
    val description = applicationContext.getString(R.string.notification__sync_description)

    createSyncNotificationChannel()

    val builder =
      NotificationCompat
        .Builder(applicationContext, NOTIFICATION_CHANNEL_ID)
        .setContentTitle(title)
        .setTicker(title)
        .setContentText(description)
        .setSmallIcon(R.drawable.ic_mbrc_status)
        .setOngoing(true)
    val notification = builder.build()

    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
      ForegroundInfo(NOTIFICATION_ID, notification, FOREGROUND_SERVICE_TYPE_DATA_SYNC)
    } else {
      ForegroundInfo(NOTIFICATION_ID, notification)
    }
  }

  private fun updateProgress(category: LibraryMediaType, current: Int, total: Int) {
    val title = applicationContext.getString(R.string.notification__sync_title)
    val contextText = applicationContext.getString(category.nameRes, current, total)
    val builder =
      NotificationCompat
        .Builder(applicationContext, NOTIFICATION_CHANNEL_ID)
        .setContentTitle(title)
        .setSmallIcon(R.drawable.ic_mbrc_status)
        .setOngoing(true)
        .setProgress(current, total, false)
        .setContentText(contextText)

    notificationManager.notify(NOTIFICATION_ID, builder.build())
  }

  override suspend fun doWork(): Result {
    val auto = inputData.getBoolean(AUTO, false)

    setForeground(createForegroundInfo())

    val syncResult =
      librarySyncUseCase.sync(auto) { category: LibraryMediaType, current: Int, total: Int ->
        updateProgress(category, current, total)
        setProgressAsync(
          workDataOf(
            CURRENT to current,
            TOTAL to total,
            CATEGORY to category.code
          )
        )
      }

    return when (syncResult) {
      is SyncResult.Success -> Result.success(syncResult.stats.toWorkData())
      SyncResult.Noop -> Result.success()

      is SyncResult.Failed -> Result.failure(workDataOf("error" to syncResult.message))
    }
  }

  companion object {
    const val AUTO = "sync_auto"
    const val CURRENT = "current"
    const val TOTAL = "total"
    const val CATEGORY = "category"

    const val SYNC_WORK_TAG = "sync_work"

    const val NOTIFICATION_ID = 819
    const val NOTIFICATION_CHANNEL_ID = "sync_channel"

    fun createWorkRequest(auto: Boolean = false): OneTimeWorkRequest {
      Timber.i("Scheduling library sync")
      val input =
        Data
          .Builder()
          .putBoolean(AUTO, auto)
          .build()

      return OneTimeWorkRequestBuilder<LibrarySyncWorker>()
        .setInputData(input)
        .build()
    }
  }
}
