package com.kelsos.mbrc.commands

import com.kelsos.mbrc.common.state.ConnectionModel
import com.kelsos.mbrc.networking.protocol.ProtocolAction
import com.kelsos.mbrc.networking.protocol.ProtocolHandler
import com.kelsos.mbrc.networking.protocol.ProtocolMessage
import javax.inject.Inject

class HandleHandshake
  @Inject
  constructor(
    private val handler: ProtocolHandler,
    private val model: ConnectionModel,
  ) : ProtocolAction {
    override fun execute(message: ProtocolMessage) {
      if (!(message.data as Boolean)) {
        handler.resetHandshake()
        model.setHandShakeDone(false)
      }
    }
  }
