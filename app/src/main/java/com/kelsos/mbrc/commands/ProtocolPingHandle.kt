package com.kelsos.mbrc.commands

import com.kelsos.mbrc.constants.Protocol
import com.kelsos.mbrc.data.SocketMessage
import com.kelsos.mbrc.interfaces.ICommand
import com.kelsos.mbrc.interfaces.IEvent
import com.kelsos.mbrc.services.SocketService
import com.kelsos.mbrc.utilities.SocketActivityChecker
import javax.inject.Inject

class ProtocolPingHandle
@Inject constructor(private val service: SocketService) : ICommand {
  @Inject
  lateinit var activityChecker: SocketActivityChecker

  override fun execute(e: IEvent) {
    activityChecker.ping()
    service.sendData(SocketMessage.create(Protocol.PONG, ""))
  }
}
