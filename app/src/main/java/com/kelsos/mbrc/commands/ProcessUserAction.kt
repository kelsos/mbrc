package com.kelsos.mbrc.commands

import com.kelsos.mbrc.data.UserAction
import com.kelsos.mbrc.networking.client.SocketMessage
import com.kelsos.mbrc.networking.client.SocketService
import com.kelsos.mbrc.networking.protocol.ProtocolAction
import com.kelsos.mbrc.networking.protocol.ProtocolMessage

class ProcessUserAction(
  private val socket: SocketService,
) : ProtocolAction {
  override fun execute(message: ProtocolMessage) {
    socket.sendData(
      SocketMessage.create(
        (message.data as UserAction).context,
        (message.data as UserAction).data,
      ),
    )
  }
}
