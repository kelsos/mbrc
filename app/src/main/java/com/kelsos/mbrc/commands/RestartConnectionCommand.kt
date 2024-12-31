package com.kelsos.mbrc.commands

import com.kelsos.mbrc.annotations.SocketAction.RESET
import com.kelsos.mbrc.networking.client.SocketService
import com.kelsos.mbrc.networking.protocol.ProtocolAction
import com.kelsos.mbrc.networking.protocol.ProtocolMessage
import javax.inject.Inject

class RestartConnectionCommand
  @Inject
  constructor(
    private val socket: SocketService,
  ) : ProtocolAction {
    override fun execute(message: ProtocolMessage) {
      socket.socketManager(RESET)
    }
  }
