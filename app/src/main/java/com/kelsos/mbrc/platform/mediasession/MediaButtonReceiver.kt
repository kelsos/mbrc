package com.kelsos.mbrc.platform.mediasession

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import toothpick.Scope
import toothpick.Toothpick
import javax.inject.Inject

class MediaButtonReceiver : BroadcastReceiver() {
  @Inject
  lateinit var handler: MediaIntentHandler
  private var scope: Scope? = null

  override fun onReceive(
    context: Context,
    intent: Intent,
  ) {
    if (scope == null) {
      scope = Toothpick.openScope(context.applicationContext)
      Toothpick.inject(this, scope)
    }

    handler.handleMediaIntent(intent)
  }
}
