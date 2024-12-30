package com.kelsos.mbrc.utilities

import android.content.Intent
import android.view.KeyEvent
import com.kelsos.mbrc.constants.Protocol
import com.kelsos.mbrc.constants.ProtocolEventType
import com.kelsos.mbrc.data.UserAction
import com.kelsos.mbrc.events.MessageEvent
import com.kelsos.mbrc.events.bus.RxBus
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MediaIntentHandler
  @Inject
  constructor(
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
        val keyEvent = extras?.get(Intent.EXTRA_KEY_EVENT) as KeyEvent?

        if (keyEvent?.action != KeyEvent.ACTION_DOWN) {
          return false
        }

        result =
          when (keyEvent.keyCode) {
            KeyEvent.KEYCODE_HEADSETHOOK -> {
              val currentClick = System.currentTimeMillis()
              if (currentClick - previousClick < DOUBLE_CLICK_INTERVAL) {
                return postAction(UserAction(Protocol.PlayerNext, true))
              }
              previousClick = currentClick
              postAction(UserAction(Protocol.PlayerPlayPause, true))
            }
            KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE ->
              postAction(UserAction(Protocol.PlayerPlayPause, true))
            KeyEvent.KEYCODE_MEDIA_PLAY -> postAction(UserAction(Protocol.PlayerPlay, true))
            KeyEvent.KEYCODE_MEDIA_PAUSE -> postAction(UserAction(Protocol.PlayerPause, true))
            KeyEvent.KEYCODE_MEDIA_STOP -> postAction(UserAction(Protocol.PlayerStop, true))
            KeyEvent.KEYCODE_MEDIA_NEXT -> postAction(UserAction(Protocol.PlayerNext, true))
            KeyEvent.KEYCODE_MEDIA_PREVIOUS -> postAction(UserAction(Protocol.PlayerPrevious, true))
            else -> false
          }
      }
      return result
    }

    private fun postAction(action: UserAction): Boolean {
      bus.post(MessageEvent(ProtocolEventType.UserAction, action))
      return true
    }

    companion object {
      private const val DOUBLE_CLICK_INTERVAL = 350
    }
  }
