package com.kelsos.mbrc.platform.mediasession

import android.app.PendingIntent
import android.app.PendingIntent.FLAG_IMMUTABLE
import android.app.PendingIntent.FLAG_UPDATE_CURRENT
import android.app.PendingIntent.getActivity
import android.app.PendingIntent.getBroadcast
import android.content.Context
import android.content.Intent
import com.kelsos.mbrc.features.player.PlayerActivity

object RemoteViewIntentBuilder {
  const val PLAY_PRESSED = "com.kelsos.mbrc.notification.play"
  const val NEXT_PRESSED = "com.kelsos.mbrc.notification.next"
  const val CLOSE_PRESSED = "com.kelsos.mbrc.notification.close"
  const val PREVIOUS_PRESSED = "com.kelsos.mbrc.notification.previous"
  const val CANCELLED_NOTIFICATION = "com.kelsos.mbrc.notification.cancel"

  fun getPendingIntent(remoteIntentCode: RemoteIntentCode, context: Context): PendingIntent {
    val code = remoteIntentCode.code
    val flag = FLAG_UPDATE_CURRENT or FLAG_IMMUTABLE

    val intent =
      when (remoteIntentCode) {
        RemoteIntentCode.Open -> Intent(context, PlayerActivity::class.java)
        RemoteIntentCode.Play -> Intent(PLAY_PRESSED)
        RemoteIntentCode.Next -> Intent(NEXT_PRESSED)
        RemoteIntentCode.Close -> Intent(CLOSE_PRESSED)
        RemoteIntentCode.Previous -> Intent(PREVIOUS_PRESSED)
        RemoteIntentCode.Cancel -> Intent(CANCELLED_NOTIFICATION)
      }

    return if (remoteIntentCode == RemoteIntentCode.Open) {
      getActivity(context, code, intent, flag)
    } else {
      getBroadcast(context, code, intent, flag)
    }
  }
}
