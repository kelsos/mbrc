package com.kelsos.mbrc.platform.mediasession

import android.content.Intent
import android.view.KeyEvent
import com.kelsos.mbrc.networking.protocol.Protocol
import com.kelsos.mbrc.networking.protocol.UserAction
import com.kelsos.mbrc.networking.protocol.UserActionUseCase
import com.kelsos.mbrc.networking.protocol.VolumeModifyUseCase
import kotlinx.coroutines.runBlocking

class MediaIntentHandler(
  private val userActionUseCase: UserActionUseCase,
  private val volumeModifyUseCase: VolumeModifyUseCase
) {
  private var previousClick: Long = 0

  private fun getKeyEventFromIntent(mediaIntent: Intent?): KeyEvent? {
    val action = mediaIntent?.action

    if (action == Intent.ACTION_MEDIA_BUTTON) {
      val extras = mediaIntent.extras
      return if (android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.TIRAMISU) {
        @Suppress("DEPRECATION")
        (extras?.getParcelable(Intent.EXTRA_KEY_EVENT))
      } else {
        extras?.getParcelable(Intent.EXTRA_KEY_EVENT, KeyEvent::class.java)
      }
    }
    return null
  }

  private fun detectDoubleClick(): Boolean {
    val currentClick = System.currentTimeMillis()
    if (currentClick - previousClick < DOUBLE_CLICK_INTERVAL) {
      return postAction(UserAction(Protocol.PlayerNext, true))
    }
    previousClick = currentClick
    return postAction(UserAction(Protocol.PlayerPlayPause, true))
  }

  fun handleMediaIntent(mediaIntent: Intent?): Boolean {
    val event = getKeyEventFromIntent(mediaIntent)
    if (event?.action != KeyEvent.ACTION_DOWN) {
      return false
    }

    return when (event.keyCode) {
      KeyEvent.KEYCODE_HEADSETHOOK -> detectDoubleClick()
      KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE -> postAction(UserAction(Protocol.PlayerPlayPause, true))
      KeyEvent.KEYCODE_MEDIA_PLAY -> postAction(UserAction(Protocol.PlayerPlay, true))
      KeyEvent.KEYCODE_MEDIA_PAUSE -> postAction(UserAction(Protocol.PlayerPause, true))
      KeyEvent.KEYCODE_MEDIA_STOP -> postAction(UserAction(Protocol.PlayerStop, true))
      KeyEvent.KEYCODE_MEDIA_NEXT -> postAction(UserAction(Protocol.PlayerNext, true))
      KeyEvent.KEYCODE_MEDIA_PREVIOUS -> postAction(UserAction(Protocol.PlayerPrevious, true))
      KeyEvent.KEYCODE_VOLUME_UP -> {
        runBlocking { volumeModifyUseCase.increase() }
        true
      }
      KeyEvent.KEYCODE_VOLUME_DOWN -> {
        runBlocking { volumeModifyUseCase.decrease() }
        true
      }
      KeyEvent.KEYCODE_VOLUME_MUTE -> postAction(UserAction.toggle(Protocol.PlayerMute))
      else -> false
    }
  }

  private fun postAction(action: UserAction): Boolean {
    userActionUseCase.tryPerform(action)
    return true
  }

  companion object {
    private const val DOUBLE_CLICK_INTERVAL = 350
  }
}
