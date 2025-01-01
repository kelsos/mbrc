package com.kelsos.mbrc.commands

import com.kelsos.mbrc.common.state.MainDataModel
import com.kelsos.mbrc.data.UserAction
import com.kelsos.mbrc.events.MessageEvent
import com.kelsos.mbrc.events.bus.RxBus
import com.kelsos.mbrc.networking.protocol.Protocol
import com.kelsos.mbrc.networking.protocol.ProtocolAction
import com.kelsos.mbrc.networking.protocol.ProtocolMessage

class KeyVolumeUpCommand(
  private val model: MainDataModel,
  private val bus: RxBus,
) : ProtocolAction {
  override fun execute(message: ProtocolMessage) {
    val volume: Int =
      if (model.volume <= 90) {
        val mod = model.volume % 10

        when {
          mod == 0 -> model.volume + 10
          mod < 5 -> model.volume + (10 - mod)
          else -> model.volume + (20 - mod)
        }
      } else {
        100
      }

    bus.post(MessageEvent.action(UserAction(Protocol.PLAYER_VOLUME, volume)))
  }
}
