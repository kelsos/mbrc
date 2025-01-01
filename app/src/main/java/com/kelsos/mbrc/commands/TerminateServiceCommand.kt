package com.kelsos.mbrc.commands

import android.app.Application
import android.content.Intent
import com.kelsos.mbrc.networking.protocol.ProtocolAction
import com.kelsos.mbrc.networking.protocol.ProtocolMessage
import com.kelsos.mbrc.platform.RemoteService

class TerminateServiceCommand(
  private val application: Application,
) : ProtocolAction {
  override fun execute(message: ProtocolMessage) {
    if (RemoteService.serviceStopping) {
      return
    }
    application.run {
      stopService(Intent(this, RemoteService::class.java))
    }
  }
}
