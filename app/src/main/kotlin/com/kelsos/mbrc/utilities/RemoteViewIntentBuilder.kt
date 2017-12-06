package com.kelsos.mbrc.utilities

import android.app.PendingIntent
import android.app.PendingIntent.FLAG_UPDATE_CURRENT
import android.content.Context
import android.content.Intent
import androidx.annotation.IntDef
import com.kelsos.mbrc.ui.navigation.main.MainActivity

object RemoteViewIntentBuilder {
  const val REMOTE_PLAY_PRESSED = "com.kelsos.mbrc.notification.play"
  const val REMOTE_NEXT_PRESSED = "com.kelsos.mbrc.notification.next"
  const val REMOTE_CLOSE_PRESSED = "com.kelsos.mbrc.notification.close"
  const val REMOTE_PREVIOUS_PRESSED = "com.kelsos.mbrc.notification.previous"
  const val CANCELLED_NOTIFICATION = "com.kelsos.mbrc.notification.cancel"
  const val OPEN = 0
  const val PLAY = 1
  const val NEXT = 2
  const val CLOSE = 3
  const val PREVIOUS = 4
  const val CANCEL = 5

  fun getPendingIntent(@ButtonAction id: Int, mContext: Context): PendingIntent {
    when (id) {
      OPEN -> {
        val notificationIntent = Intent(mContext, MainActivity::class.java)
        return PendingIntent.getActivity(
          mContext,
          0,
          notificationIntent,
          FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
      }
      PLAY -> {
        val playPressedIntent = Intent(REMOTE_PLAY_PRESSED)
        return PendingIntent.getBroadcast(
          mContext,
          1,
          playPressedIntent,
          FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
      }
      NEXT -> {
        val mediaNextButtonIntent = Intent(REMOTE_NEXT_PRESSED)
        return PendingIntent.getBroadcast(
          mContext,
          2,
          mediaNextButtonIntent,
          FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
      }
      CLOSE -> {
        val clearNotificationIntent = Intent(REMOTE_CLOSE_PRESSED)
        return PendingIntent.getBroadcast(
          mContext,
          3,
          clearNotificationIntent,
          FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
      }
      PREVIOUS -> {
        val mediaPreviousButtonIntent = Intent(REMOTE_PREVIOUS_PRESSED)
        return PendingIntent.getBroadcast(
          mContext,
          4,
          mediaPreviousButtonIntent,
          FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
      }
      CANCEL -> {
        val cancelIntent = Intent(CANCELLED_NOTIFICATION)
        return PendingIntent.getBroadcast(
          mContext,
          4,
          cancelIntent,
          FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
      }
      else -> throw IndexOutOfBoundsException()
    }
  }

  @IntDef(
    OPEN,
    PLAY,
    CLOSE,
    PREVIOUS,
    NEXT,
    CANCEL
  )
  @kotlin.annotation.Retention(AnnotationRetention.SOURCE)
  annotation class ButtonAction
}
