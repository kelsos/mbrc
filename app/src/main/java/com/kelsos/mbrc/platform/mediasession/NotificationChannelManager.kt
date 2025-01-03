package com.kelsos.mbrc.platform.mediasession

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationManagerCompat
import com.kelsos.mbrc.R

/**
 * Manages notification channels for the application.
 */
class NotificationChannelManager(
  private val context: Context,
) {
  /**
   * Creates a notification channel for the media session notifications.
   * This is only required for Android O and above.
   */
  @RequiresApi(Build.VERSION_CODES.O)
  fun createChannel(): NotificationChannel {
    val channelName = context.getString(R.string.notification__session_channel_name)
    val channelDescription = context.getString(R.string.notification__session_channel_description)

    val channel =
      NotificationChannel(
        AppNotificationManager.CHANNEL_ID,
        channelName,
        NotificationManager.IMPORTANCE_DEFAULT,
      )

    return channel.apply {
      this.description = channelDescription
      enableLights(false)
      enableVibration(false)
      setSound(null, null)
    }
  }

  /**
   * Creates and registers the notification channel with the system.
   * This is only required for Android O and above.
   */
  fun ensureChannelExists() {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
      val channel = createChannel()
      val manager = NotificationManagerCompat.from(context)
      manager.createNotificationChannel(channel)
    }
  }
}
