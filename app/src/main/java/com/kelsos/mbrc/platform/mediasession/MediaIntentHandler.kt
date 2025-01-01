package com.kelsos.mbrc.platform.mediasession

import android.content.Intent
import android.view.KeyEvent
import androidx.core.os.BundleCompat
import com.kelsos.mbrc.constants.ProtocolEventType
import com.kelsos.mbrc.data.UserAction
import com.kelsos.mbrc.events.MessageEvent
import com.kelsos.mbrc.events.bus.RxBus
import com.kelsos.mbrc.networking.protocol.Protocol

class MediaIntentHandler(
  private val bus: RxBus,
) {
  private var previousClick: Long = 0

  init {
    previousClick = 0
  }

  fun handleMediaIntent(mediaIntent: Intent?): Boolean {
    var result = false
    if (mediaIntent?.action == Intent.ACTION_MEDIA_BUTTON) {
      val extras = mediaIntent.extras
      if (extras == null) {
        return false
      }
      val keyEvent = BundleCompat.getParcelable<KeyEvent>(extras, Intent.EXTRA_KEY_EVENT, KeyEvent::class.java)

      if (keyEvent?.action != KeyEvent.ACTION_DOWN) {
        return false
      }

      result =
        when (keyEvent.keyCode) {
          KeyEvent.KEYCODE_HEADSETHOOK -> {
            val currentClick = System.currentTimeMillis()
            if (currentClick - previousClick < DOUBLE_CLICK_INTERVAL) {
              return postAction(UserAction(Protocol.PLAYER_NEXT, true))
            }
            previousClick = currentClick
            postAction(UserAction(Protocol.PLAYER_PLAY_PAUSE, true))
          }
          KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE ->
            postAction(UserAction(Protocol.PLAYER_PLAY_PAUSE, true))
          KeyEvent.KEYCODE_MEDIA_PLAY -> postAction(UserAction(Protocol.PLAYER_PLAY, true))
          KeyEvent.KEYCODE_MEDIA_PAUSE -> postAction(UserAction(Protocol.PLAYER_PAUSE, true))
          KeyEvent.KEYCODE_MEDIA_STOP -> postAction(UserAction(Protocol.PLAYER_STOP, true))
          KeyEvent.KEYCODE_MEDIA_NEXT -> postAction(UserAction(Protocol.PLAYER_NEXT, true))
          KeyEvent.KEYCODE_MEDIA_PREVIOUS -> postAction(UserAction(Protocol.PLAYER_PREVIOUS, true))
          else -> false
        }
    }
    return result
  }

  private fun postAction(action: UserAction): Boolean {
    bus.post(MessageEvent(ProtocolEventType.USER_ACTION, action))
    return true
  }

  companion object {
    private const val DOUBLE_CLICK_INTERVAL = 350
  }
}
