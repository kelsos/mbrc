package com.kelsos.mbrc.platform

import android.Manifest
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.telephony.TelephonyManager
import com.kelsos.mbrc.constants.ProtocolEventType
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
      addAction(RemoteViewIntentBuilder.PLAY_PRESSED)
      addAction(RemoteViewIntentBuilder.NEXT_PRESSED)
      addAction(RemoteViewIntentBuilder.CLOSE_PRESSED)
      addAction(RemoteViewIntentBuilder.PREVIOUS_PRESSED)
      addAction(RemoteViewIntentBuilder.CANCELLED_NOTIFICATION)
    }
  }

  override fun onReceive(
    context: Context,
    intent: Intent,
  ) {
    Timber.v("Incoming %s", intent)
    when (intent.action) {
      TelephonyManager.ACTION_PHONE_STATE_CHANGED -> {
        Timber.v("Incoming")
        val bundle = intent.extras ?: return
        val state = bundle.getString(TelephonyManager.EXTRA_STATE)
        if (TelephonyManager.EXTRA_STATE_RINGING.equals(state!!, ignoreCase = true)) {
          handleRinging()
        }
      }

      RemoteViewIntentBuilder.PLAY_PRESSED -> performAction(Protocol.PLAYER_PLAY_PAUSE)
      RemoteViewIntentBuilder.NEXT_PRESSED -> performAction(Protocol.PLAYER_NEXT)
      RemoteViewIntentBuilder.CLOSE_PRESSED -> stopService(context)
      RemoteViewIntentBuilder.PREVIOUS_PRESSED -> performAction(Protocol.PLAYER_PREVIOUS)
      RemoteViewIntentBuilder.CANCELLED_NOTIFICATION -> stopService(context)
    }
  }

  private fun stopService(context: Context) {
    if (!RemoteService.serviceStopping) {
      context.stopService(Intent(context, RemoteService::class.java))
    }
  }

  private fun handleRinging() {
    when (settingsManager.getCallAction()) {
      SettingsManager.Companion.PAUSE -> performAction(Protocol.PLAYER_PAUSE)
      SettingsManager.Companion.STOP -> performAction(Protocol.PLAYER_STOP)
      SettingsManager.Companion.REDUCE -> bus.post(MessageEvent(ProtocolEventType.REDUCE_VOLUME))
    }
  }

  // TODO move to sealed classes to ensure type safety
  private fun performAction(protocol: String) {
    bus.post(MessageEvent(ProtocolEventType.USER_ACTION, UserAction(protocol, true)))
  }
}
