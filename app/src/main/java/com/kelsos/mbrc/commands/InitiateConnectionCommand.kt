package com.kelsos.mbrc.commands

import com.kelsos.mbrc.annotations.SocketAction.START
import com.kelsos.mbrc.networking.client.SocketService
import com.kelsos.mbrc.networking.protocol.ProtocolAction
import com.kelsos.mbrc.networking.protocol.ProtocolMessage
import javax.inject.Inject

class InitiateConnectionCommand
  @Inject
  constructor(
    private val socketService: SocketService,
  ) : ProtocolAction {
    override fun execute(message: ProtocolMessage) {
      socketService.socketManager(START)
    }
  }
