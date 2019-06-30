package com.kelsos.mbrc.platform.mediasession

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import org.koin.core.KoinComponent
import org.koin.core.inject

class MediaButtonReceiver : BroadcastReceiver(), KoinComponent {

  private val handler: MediaIntentHandler by inject()

  override fun onReceive(context: Context, intent: Intent) {
    handler.handleMediaIntent(intent)
  }
}