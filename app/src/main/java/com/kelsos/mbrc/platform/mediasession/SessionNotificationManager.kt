package com.kelsos.mbrc.platform.mediasession

import android.app.Application
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.graphics.BitmapFactory
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationCompat.Action
import com.kelsos.mbrc.R
import com.kelsos.mbrc.common.utilities.AppDispatchers
import com.kelsos.mbrc.common.utilities.RemoteUtils
import com.kelsos.mbrc.content.activestatus.PlayerState
import com.kelsos.mbrc.features.library.PlayingTrack
import com.kelsos.mbrc.platform.ForegroundHooks
import com.kelsos.mbrc.platform.mediasession.INotificationManager.Companion.CHANNEL_ID
import com.kelsos.mbrc.platform.mediasession.INotificationManager.Companion.NOW_PLAYING_PLACEHOLDER
import com.kelsos.mbrc.platform.mediasession.RemoteViewIntentBuilder.getPendingIntent
import com.kelsos.mbrc.preferences.SettingsManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class SessionNotificationManager(
  private val context: Application,
  private val sessionManager: RemoteSessionManager,
  private val settings: SettingsManager,
  private val dispatchers: AppDispatchers,
  private val notificationManager: NotificationManager
) : INotificationManager {

  private val sessionJob: Job = Job()
  private val uiScope: CoroutineScope = CoroutineScope(dispatchers.main + sessionJob)
  private val diskScope: CoroutineScope = CoroutineScope(dispatchers.io + sessionJob)

  private val previous: String by lazy { context.getString(R.string.notification_action_previous) }
  private val play: String by lazy { context.getString(R.string.notification_action_play) }
  private val next: String by lazy { context.getString(R.string.notification_action_next) }

  private val channelName by lazy { context.getString(R.string.notification__session_channel_name) }
  private val channelDescription by lazy {
    context.getString(R.string.notification__session_channel_description)
  }

  private var notification: Notification? = null
  private var hooks: ForegroundHooks? = null

  private var notificationData: NotificationData = NotificationData()

  init {
    createNotificationChannels()
  }

  private suspend fun update(notificationData: NotificationData) {
    notification = createBuilder(notificationData).build()

    withContext(dispatchers.main) {
      notificationManager.notify(NOW_PLAYING_PLACEHOLDER, notification)
    }
  }

  private fun createNotificationChannels() {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
      return
    }

    val channel = NotificationChannel(
      CHANNEL_ID,
      channelName,
      NotificationManager.IMPORTANCE_DEFAULT
    )

    channel.apply {
      this.description = channelDescription
      enableLights(false)
      enableVibration(false)
      setSound(null, null)
    }

    notificationManager.createNotificationChannel(channel)
  }

  private fun createBuilder(notificationData: NotificationData): NotificationCompat.Builder {
    val mediaStyle = androidx.media.app.NotificationCompat.MediaStyle()
    mediaStyle.setMediaSession(sessionManager.mediaSessionToken)

    val builder = NotificationCompat.Builder(context, CHANNEL_ID)
    val resId = if (notificationData.playerState == PlayerState.Playing) {
      R.drawable.ic_action_pause
    } else {
      R.drawable.ic_action_play
    }

    builder.setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
      .setSmallIcon(R.drawable.ic_mbrc_status)
      .setStyle(mediaStyle.setShowActionsInCompactView(1, 2))
      .addAction(getPreviousAction())
      .addAction(getPlayAction(resId))
      .addAction(getNextAction())

    builder.priority = NotificationCompat.PRIORITY_LOW
    builder.setOnlyAlertOnce(true)

    if (notificationData.cover != null) {
      builder.setLargeIcon(this.notificationData.cover)
    } else {
      val icon = BitmapFactory.decodeResource(context.resources, R.drawable.ic_image_no_cover)
      builder.setLargeIcon(icon)
    }

    with(notificationData.track) {
      builder.setContentTitle(title)
        .setContentText(artist)
        .setSubText(album)
    }

    builder.setContentIntent(getPendingIntent(RemoteIntentCode.Open, context))

    return builder
  }

  private fun getPreviousAction(): Action {
    val previousIntent = getPendingIntent(RemoteIntentCode.Previous, context)
    return Action.Builder(R.drawable.ic_action_previous, previous, previousIntent).build()
  }

  private fun getPlayAction(playStateIcon: Int): Action {
    val playIntent = getPendingIntent(RemoteIntentCode.Play, context)

    return Action.Builder(playStateIcon, play, playIntent).build()
  }

  private fun getNextAction(): Action {
    val nextIntent = getPendingIntent(RemoteIntentCode.Next, context)
    return Action.Builder(R.drawable.ic_action_next, next, nextIntent).build()
  }

  override fun cancel(notificationId: Int) {
    notificationManager.cancel(notificationId)
    hooks?.stop()
  }

  override fun setForegroundHooks(hooks: ForegroundHooks) {
    this.hooks = hooks
  }

  override fun trackChanged(playingTrack: PlayingTrack) {
    diskScope.launch {
      notificationData = with(playingTrack.coverUrl) {
        val cover = if (isNotEmpty()) {
          RemoteUtils.loadBitmap(this).fold({ null }) { bitmap -> bitmap }
        } else {
          null
        }

        notificationData.copy(track = playingTrack, cover = cover)
      }

      update(notificationData)
    }
  }

  override fun connectionStateChanged(connected: Boolean) {
    if (!connected) {
      cancel(NOW_PLAYING_PLACEHOLDER)
    } else {
      notification = createBuilder(notificationData).build().also {
        hooks?.start(NOW_PLAYING_PLACEHOLDER, it)
      }
    }
  }

  override fun playerStateChanged(state: PlayerState) {
    if (notificationData.playerState == state) {
      return
    }

    uiScope.launch {
      notificationData = notificationData.copy(playerState = state)
      update(notificationData)
    }
  }
}
