package com.kelsos.mbrc.commands

import android.app.Application
import android.content.Intent
import com.kelsos.mbrc.controller.RemoteService
import com.kelsos.mbrc.interfaces.ICommand
import com.kelsos.mbrc.interfaces.IEvent
import javax.inject.Inject

class TerminateServiceCommand
@Inject constructor(
  private val application: Application
) : ICommand {

  override fun execute(e: IEvent) {
    if (RemoteService.SERVICE_STOPPING) {
      return
    }
    application.run {
      stopService(Intent(this, RemoteService::class.java))
    }
  }
}
