package com.kelsos.mbrc.commands

import com.kelsos.mbrc.networking.client.SocketMessage
import com.kelsos.mbrc.networking.client.SocketService
import com.kelsos.mbrc.networking.protocol.Protocol
import com.kelsos.mbrc.networking.protocol.ProtocolAction
import com.kelsos.mbrc.networking.protocol.ProtocolMessage
import com.kelsos.mbrc.networking.protocol.ProtocolPayload
import javax.inject.Inject

class ProtocolRequest
  @Inject
  constructor(
    private val socket: SocketService,
  ) : ProtocolAction {
    override fun execute(message: ProtocolMessage) {
      val payload = ProtocolPayload()
      payload.noBroadcast = false
      payload.protocolVersion = Protocol.PROTOCOL_VERSION_NUMBER
      socket.sendData(SocketMessage.create(Protocol.PROTOCOL_TAG, payload))
    }
  }
