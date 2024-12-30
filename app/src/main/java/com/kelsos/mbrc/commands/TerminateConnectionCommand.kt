package com.kelsos.mbrc.commands

import com.kelsos.mbrc.annotations.SocketAction.TERMINATE
import com.kelsos.mbrc.interfaces.ICommand
import com.kelsos.mbrc.interfaces.IEvent
import com.kelsos.mbrc.model.ConnectionModel
import com.kelsos.mbrc.services.SocketService
import javax.inject.Inject

class TerminateConnectionCommand
  @Inject
  constructor(
    private val service: SocketService,
    private val model: ConnectionModel,
  ) : ICommand {
    override fun execute(e: IEvent) {
      model.setHandShakeDone(false)
      model.setConnectionState("false")
      service.socketManager(TERMINATE)
    }
  }
