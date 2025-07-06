package com.kelsos.mbrc.platform.mediasession

import android.content.Context
import android.graphics.BitmapFactory
import androidx.annotation.OptIn
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat.getString
import androidx.media3.common.util.UnstableApi
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaStyleNotificationHelper
import com.kelsos.mbrc.R
import com.kelsos.mbrc.platform.mediasession.RemoteViewIntentBuilder.getPendingIntent

/**
 * Builds notifications for the media session.
 */
class NotificationBuilder(
  private val context: Context,
  private val actionManager: NotificationActionManager
) {
  /**
   * Creates a notification builder for the given notification data and media session.
   *
   * @param notificationData The data to use for the notification.
   * @param mediaSession The media session to use for the notification.
   * @return A configured notification builder.
   */
  @OptIn(UnstableApi::class)
  fun createBuilder(
    notificationData: NotificationData,
    mediaSession: MediaSession
  ): NotificationCompat.Builder {
    val style =
      MediaStyleNotificationHelper
        .MediaStyle(mediaSession)
        .setShowActionsInCompactView(0, 1, 2)
    val builder = NotificationCompat.Builder(context, AppNotificationManager.CHANNEL_ID)

    builder
      .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
      .setSmallIcon(R.drawable.ic_mbrc_status)
      .setStyle(style)

    builder.priority = NotificationCompat.PRIORITY_LOW
    builder.setOnlyAlertOnce(true)

    if (notificationData.cover != null) {
      builder.setLargeIcon(notificationData.cover)
    } else {
      val icon = BitmapFactory.decodeResource(context.resources, R.drawable.ic_image_no_cover)
      builder.setLargeIcon(icon)
    }

    with(notificationData.track) {
      builder
        .setContentTitle(title)
        .setContentText(artist)
        .setSubText(album)
    }

    builder.setContentIntent(getPendingIntent(RemoteIntentCode.Open, context))

    actionManager.addMediaActions(builder, notificationData.playerState)

    return builder
  }

  /**
   * Creates a placeholder notification builder.
   *
   * @return A configured notification builder for a placeholder notification.
   */
  fun createPlaceholderBuilder(): NotificationCompat.Builder {
    val builder =
      NotificationCompat
        .Builder(context, AppNotificationManager.CHANNEL_ID)
        .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
        .setSmallIcon(R.drawable.ic_mbrc_status)
        .setContentTitle(getString(context, R.string.application_name))
        .setContentText(getString(context, R.string.application_starting))
        .addAction(actionManager.createCancelAction())

    return builder
  }
}
