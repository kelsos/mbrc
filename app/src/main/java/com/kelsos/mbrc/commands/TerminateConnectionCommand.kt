package com.kelsos.mbrc.commands

import com.kelsos.mbrc.annotations.SocketAction.TERMINATE
import com.kelsos.mbrc.common.state.ConnectionModel
import com.kelsos.mbrc.networking.client.SocketService
import com.kelsos.mbrc.networking.protocol.ProtocolAction
import com.kelsos.mbrc.networking.protocol.ProtocolMessage
import javax.inject.Inject

class TerminateConnectionCommand
  @Inject
  constructor(
    private val service: SocketService,
    private val model: ConnectionModel,
  ) : ProtocolAction {
    override fun execute(message: ProtocolMessage) {
      model.setHandShakeDone(false)
      model.setConnectionState("false")
      service.socketManager(TERMINATE)
    }
  }
