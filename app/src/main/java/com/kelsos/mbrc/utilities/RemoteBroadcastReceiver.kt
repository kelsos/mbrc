package com.kelsos.mbrc.utilities

import android.Manifest.permission.READ_PHONE_STATE
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.telephony.TelephonyManager
import com.kelsos.mbrc.constants.Protocol
import com.kelsos.mbrc.constants.ProtocolEventType
import com.kelsos.mbrc.constants.UserInputEventType
import com.kelsos.mbrc.controller.RemoteService
import com.kelsos.mbrc.data.UserAction
import com.kelsos.mbrc.events.MessageEvent
import com.kelsos.mbrc.events.bus.RxBus
import timber.log.Timber
import javax.inject.Inject

class RemoteBroadcastReceiver
  @Inject
  constructor(
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
        context.checkSelfPermission(READ_PHONE_STATE) == PackageManager.PERMISSION_GRANTED
      val handleCallAction = settingsManager.getCallAction() != SettingsManager.NONE

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
        RemoteViewIntentBuilder.REMOTE_PLAY_PRESSED -> {
          postAction(UserAction(Protocol.PlayerPlayPause, true))
        }
        RemoteViewIntentBuilder.REMOTE_NEXT_PRESSED -> {
          postAction(UserAction(Protocol.PlayerNext, true))
        }
        RemoteViewIntentBuilder.REMOTE_CLOSE_PRESSED -> {
          bus.post(MessageEvent(UserInputEventType.CancelNotification))
        }
        RemoteViewIntentBuilder.REMOTE_PREVIOUS_PRESSED -> {
          postAction(UserAction(Protocol.PlayerPrevious, true))
        }
        RemoteViewIntentBuilder.CANCELLED_NOTIFICATION -> {
          if (!RemoteService.SERVICE_STOPPING) {
            context.stopService(Intent(context, RemoteService::class.java))
          }
        }
      }
    }

    private fun handleRinging() {
      when (settingsManager.getCallAction()) {
        SettingsManager.PAUSE -> postAction(UserAction(Protocol.PlayerPause, true))
        SettingsManager.STOP -> postAction(UserAction(Protocol.PlayerStop, true))
        SettingsManager.REDUCE -> bus.post(MessageEvent(ProtocolEventType.ReduceVolume))
      }
    }

    private fun postAction(data: UserAction) {
      bus.post(MessageEvent(ProtocolEventType.UserAction, data))
    }
  }
