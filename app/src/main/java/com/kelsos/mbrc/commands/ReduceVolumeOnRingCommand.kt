package com.kelsos.mbrc.commands

import com.kelsos.mbrc.common.state.MainDataModel
import com.kelsos.mbrc.networking.client.SocketMessage
import com.kelsos.mbrc.networking.client.SocketService
import com.kelsos.mbrc.networking.protocol.Protocol
import com.kelsos.mbrc.networking.protocol.ProtocolAction
import com.kelsos.mbrc.networking.protocol.ProtocolMessage

class ReduceVolumeOnRingCommand(
  private val model: MainDataModel,
  private val service: SocketService,
) : ProtocolAction {
  override fun execute(message: ProtocolMessage) {
    if (model.isMute || model.volume == 0) {
      return
    }
    service.sendData(SocketMessage.create(Protocol.PLAYER_VOLUME, (model.volume * 0.2).toInt()))
  }
}
