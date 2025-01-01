package com.kelsos.mbrc.platform

import android.Manifest
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.telephony.TelephonyManager
import com.kelsos.mbrc.constants.ProtocolEventType
import com.kelsos.mbrc.constants.UserInputEventType
import com.kelsos.mbrc.data.UserAction
import com.kelsos.mbrc.events.MessageEvent
import com.kelsos.mbrc.events.bus.RxBus
import com.kelsos.mbrc.features.settings.SettingsManager
import com.kelsos.mbrc.networking.protocol.Protocol
import com.kelsos.mbrc.platform.mediasession.RemoteViewIntentBuilder
import timber.log.Timber

class RemoteBroadcastReceiver(
  private val settingsManager: SettingsManager,
  private val bus: RxBus,
) : BroadcastReceiver() {
  /**
   * Initialized and installs the IntentFilter listening for the SONG_CHANGED
   * intent fired by the ReplyHandler or the PHONE_STATE intent fired by the
   * Android operating system.
   */
  fun filter(context: Context): IntentFilter {
    val hasPermission =
      context.checkSelfPermission(Manifest.permission.READ_PHONE_STATE) == PackageManager.PERMISSION_GRANTED
    val handleCallAction = settingsManager.getCallAction() != SettingsManager.Companion.NONE

    return IntentFilter().apply {
      if (hasPermission && handleCallAction) {
        addAction(TelephonyManager.ACTION_PHONE_STATE_CHANGED)
      }
      addAction(RemoteViewIntentBuilder.REMOTE_PLAY_PRESSED)
      addAction(RemoteViewIntentBuilder.REMOTE_NEXT_PRESSED)
      addAction(RemoteViewIntentBuilder.REMOTE_CLOSE_PRESSED)
      addAction(RemoteViewIntentBuilder.REMOTE_PREVIOUS_PRESSED)
      addAction(RemoteViewIntentBuilder.CANCELLED_NOTIFICATION)
    }
  }

  override fun onReceive(
    context: Context,
    intent: Intent,
  ) {
    Timber.Forest.v("Incoming %s", intent)
    when (intent.action) {
      TelephonyManager.ACTION_PHONE_STATE_CHANGED -> {
        Timber.Forest.v("Incoming")
        val bundle = intent.extras ?: return
        val state = bundle.getString(TelephonyManager.EXTRA_STATE)
        if (TelephonyManager.EXTRA_STATE_RINGING.equals(state!!, ignoreCase = true)) {
          handleRinging()
        }
      }
      RemoteViewIntentBuilder.REMOTE_PLAY_PRESSED -> {
        postAction(UserAction(Protocol.PLAYER_PLAY_PAUSE, true))
      }
      RemoteViewIntentBuilder.REMOTE_NEXT_PRESSED -> {
        postAction(UserAction(Protocol.PLAYER_NEXT, true))
      }
      RemoteViewIntentBuilder.REMOTE_CLOSE_PRESSED -> {
        bus.post(MessageEvent(UserInputEventType.CANCEL_NOTIFICATION))
      }
      RemoteViewIntentBuilder.REMOTE_PREVIOUS_PRESSED -> {
        postAction(UserAction(Protocol.PLAYER_PREVIOUS, true))
      }
      RemoteViewIntentBuilder.CANCELLED_NOTIFICATION -> {
        if (!RemoteService.Companion.serviceStopping) {
          context.stopService(Intent(context, RemoteService::class.java))
        }
      }
    }
  }

  private fun handleRinging() {
    when (settingsManager.getCallAction()) {
      SettingsManager.Companion.PAUSE -> postAction(UserAction(Protocol.PLAYER_PAUSE, true))
      SettingsManager.Companion.STOP -> postAction(UserAction(Protocol.PLAYER_STOP, true))
      SettingsManager.Companion.REDUCE -> bus.post(MessageEvent(ProtocolEventType.REDUCE_VOLUME))
    }
  }

  private fun postAction(data: UserAction) {
    bus.post(MessageEvent(ProtocolEventType.USER_ACTION, data))
  }
}
