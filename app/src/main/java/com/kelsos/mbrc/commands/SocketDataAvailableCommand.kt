package com.kelsos.mbrc.commands

import com.kelsos.mbrc.networking.protocol.ProtocolAction
import com.kelsos.mbrc.networking.protocol.ProtocolHandler
import com.kelsos.mbrc.networking.protocol.ProtocolMessage
import javax.inject.Inject

class SocketDataAvailableCommand
  @Inject
  constructor(
    private val handler: ProtocolHandler,
  ) : ProtocolAction {
    override fun execute(message: ProtocolMessage) {
      handler.preProcessIncoming(message.dataString)
    }
  }
