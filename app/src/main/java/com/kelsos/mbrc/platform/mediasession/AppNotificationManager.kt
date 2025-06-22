package com.kelsos.mbrc.platform.mediasession

import android.app.Notification
import android.app.NotificationManager
import androidx.core.net.toUri
import com.kelsos.mbrc.common.state.Duration
import com.kelsos.mbrc.common.state.PlayerState
import com.kelsos.mbrc.common.state.PlayingTrack
import com.kelsos.mbrc.common.utilities.AppCoroutineDispatchers
import com.kelsos.mbrc.common.utilities.RemoteUtils
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

interface AppNotificationManager {
  fun initialize()

  fun destroy()

  fun cancel(notificationId: Int = MEDIA_SESSION_NOTIFICATION_ID)

  fun updatePlayingTrack(playingTrack: PlayingTrack)

  fun updateState(
    state: PlayerState,
    current: Duration,
  )

  fun connectionStateChanged(connected: Boolean)

  fun createPlaceholder(): Notification

  companion object {
    const val MEDIA_SESSION_NOTIFICATION_ID = 15613
    const val CHANNEL_ID = "mbrc_session_01"
  }
}

class AppNotificationManagerImpl(
  private val dispatchers: AppCoroutineDispatchers,
  private val notificationManager: NotificationManager,
  private val notificationBuilder: NotificationBuilder,
  private val channelManager: NotificationChannelManager,
  private val mediaSessionManager: MediaSessionManager,
) : AppNotificationManager {
  private var notification: Notification? = null
  private var notificationData: NotificationData = NotificationData()

  init {
    channelManager.ensureChannelExists()
  }

  private suspend fun update(notificationData: NotificationData) {
    val mediaSession = mediaSessionManager.mediaSession ?: return
    notification = notificationBuilder.createBuilder(notificationData, mediaSession).build()

    withContext(dispatchers.main) {
      notificationManager.notify(AppNotificationManager.MEDIA_SESSION_NOTIFICATION_ID, notification)
    }
  }

  override fun initialize() {
    mediaSessionManager.initialize()
  }

  override fun destroy() {
    mediaSessionManager.destroy()
  }

  override fun cancel(notificationId: Int) {
    notificationManager.cancel(notificationId)
  }

  override fun updatePlayingTrack(playingTrack: PlayingTrack) {
    mediaSessionManager.scope.launch {
      val coverUrl = playingTrack.coverUrl
      val cover =
        if (coverUrl.isEmpty()) {
          null
        } else {
          val uri = coverUrl.toUri()
          RemoteUtils.loadBitmap(checkNotNull(uri.path)).getOrNull()
        }
      notificationData = notificationData.copy(track = playingTrack, cover = cover)
      update(notificationData)
    }
  }

  override fun connectionStateChanged(connected: Boolean) {
    if (connected) {
      val mediaSession = mediaSessionManager.mediaSession ?: return
      notification = notificationBuilder.createBuilder(this.notificationData, mediaSession).build()
      notificationManager.notify(AppNotificationManager.MEDIA_SESSION_NOTIFICATION_ID, notification)
    } else {
      cancel(AppNotificationManager.MEDIA_SESSION_NOTIFICATION_ID)
    }
  }

  override fun updateState(
    state: PlayerState,
    current: Duration,
  ) {
    mediaSessionManager.scope.launch {
      notificationData = notificationData.copy(playerState = state)
      update(notificationData)
    }
  }

  override fun createPlaceholder(): Notification {
    channelManager.ensureChannelExists()
    return notificationBuilder.createPlaceholderBuilder().build()
  }
}
