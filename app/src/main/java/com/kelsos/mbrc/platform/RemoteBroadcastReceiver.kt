package com.kelsos.mbrc.platform

import android.Manifest
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.telephony.TelephonyManager
import com.kelsos.mbrc.features.settings.CallAction
import com.kelsos.mbrc.features.settings.SettingsManager
import com.kelsos.mbrc.networking.protocol.Protocol
import com.kelsos.mbrc.networking.protocol.UserAction
import com.kelsos.mbrc.networking.protocol.UserActionUseCase
import com.kelsos.mbrc.networking.protocol.VolumeModifyUseCase
import com.kelsos.mbrc.platform.mediasession.RemoteViewIntentBuilder
import kotlinx.coroutines.runBlocking
import timber.log.Timber

class RemoteBroadcastReceiver(
  private val settingsManager: SettingsManager,
  private val userActionUseCase: UserActionUseCase,
  private val volumeModifyUseCase: VolumeModifyUseCase
) : BroadcastReceiver() {
  /**
   * Initialized and installs the IntentFilter listening for the SONG_CHANGED
   * intent fired by the ReplyHandler or the PHONE_STATE intent fired by the
   * Android operating system.
   */
  fun filter(context: Context): IntentFilter {
    val hasPermission =
      context.checkSelfPermission(Manifest.permission.READ_PHONE_STATE) ==
        PackageManager.PERMISSION_GRANTED
    val handleCallAction = settingsManager.getCallAction() != CallAction.None

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

  override fun onReceive(context: Context, intent: Intent) {
    Timber.v("Incoming %s", intent)
    when (intent.action) {
      TelephonyManager.ACTION_PHONE_STATE_CHANGED -> {
        Timber.v("Incoming")
        val bundle = intent.extras ?: return
        val state = bundle.getString(TelephonyManager.EXTRA_STATE)
        if (TelephonyManager.EXTRA_STATE_RINGING.equals(state, ignoreCase = true)) {
          handleRinging()
        }
      }
      RemoteViewIntentBuilder.PLAY_PRESSED -> performAction(Protocol.PlayerPlayPause)
      RemoteViewIntentBuilder.NEXT_PRESSED -> performAction(Protocol.PlayerNext)
      RemoteViewIntentBuilder.CLOSE_PRESSED -> stopService(context)
      RemoteViewIntentBuilder.PREVIOUS_PRESSED -> performAction(Protocol.PlayerPrevious)
      RemoteViewIntentBuilder.CANCELLED_NOTIFICATION -> stopService(context)
    }
  }

  private fun stopService(context: Context) {
    if (!RemoteService.serviceStopping) {
      val intent = Intent(context, RemoteService::class.java)
      context.stopService(intent)
    }
  }

  private fun handleRinging() {
    when (settingsManager.getCallAction()) {
      CallAction.Pause -> performAction(Protocol.PlayerPause)
      CallAction.Stop -> performAction(Protocol.PlayerStop)
      CallAction.Reduce -> runBlocking { volumeModifyUseCase.reduceVolume() }
      else -> Timber.v("No call action set, nothing to do.")
    }
  }

  private fun performAction(protocol: Protocol) {
    userActionUseCase.tryPerform(UserAction.create(protocol))
  }
}
