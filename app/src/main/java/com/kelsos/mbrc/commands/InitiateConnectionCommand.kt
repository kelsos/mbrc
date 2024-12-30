package com.kelsos.mbrc.commands

import com.kelsos.mbrc.annotations.SocketAction.START
import com.kelsos.mbrc.interfaces.ICommand
import com.kelsos.mbrc.interfaces.IEvent
import com.kelsos.mbrc.services.SocketService
import javax.inject.Inject

class InitiateConnectionCommand
@Inject constructor(private val socketService: SocketService) : ICommand {

  override fun execute(e: IEvent) {
    socketService.socketManager(START)
  }
}
