package com.kelsos.mbrc.platform.mediasession

import android.content.Context
import androidx.core.app.NotificationCompat.Action
import androidx.core.content.ContextCompat.getString
import com.kelsos.mbrc.R
import com.kelsos.mbrc.common.state.PlayerState
import com.kelsos.mbrc.platform.mediasession.RemoteViewIntentBuilder.getPendingIntent

/**
 * Manages notification actions for the media session notifications.
 */
class NotificationActionManager(private val context: Context) {
  /**
   * Adds media control actions to the notification builder.
   *
   * @param builder The notification builder to add actions to.
   * @param playerState The current state of the player.
   */
  fun addMediaActions(
    builder: androidx.core.app.NotificationCompat.Builder,
    playerState: PlayerState
  ) {
    val previousAction =
      Action
        .Builder(
          R.drawable.baseline_skip_previous_24,
          context.getString(R.string.action_previous),
          getPendingIntent(RemoteIntentCode.Previous, context)
        ).build()
    builder.addAction(previousAction)

    val playPauseIcon =
      if (playerState == PlayerState.Playing) {
        R.drawable.baseline_pause_24
      } else {
        R.drawable.baseline_play_arrow_24
      }
    val playPauseText =
      if (playerState == PlayerState.Playing) {
        context.getString(R.string.action_pause)
      } else {
        context.getString(R.string.action_play)
      }
    val playPauseAction =
      Action
        .Builder(
          playPauseIcon,
          playPauseText,
          getPendingIntent(RemoteIntentCode.Play, context)
        ).build()
    builder.addAction(playPauseAction)

    val nextAction =
      Action
        .Builder(
          R.drawable.baseline_skip_next_24,
          context.getString(R.string.action_next),
          getPendingIntent(RemoteIntentCode.Next, context)
        ).build()
    builder.addAction(nextAction)
  }

  /**
   * Creates a cancel action for the notification.
   *
   * @return The cancel action.
   */
  fun createCancelAction(): Action {
    val cancelIntent = getPendingIntent(RemoteIntentCode.Cancel, context)
    return Action
      .Builder(
        R.drawable.baseline_close_24,
        getString(context, android.R.string.cancel),
        cancelIntent
      ).build()
  }
}
