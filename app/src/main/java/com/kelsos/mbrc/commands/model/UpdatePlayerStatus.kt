package com.kelsos.mbrc.commands.model

import com.fasterxml.jackson.databind.node.ObjectNode
import com.kelsos.mbrc.constants.Protocol
import com.kelsos.mbrc.events.bus.RxBus
import com.kelsos.mbrc.events.ui.RepeatChange
import com.kelsos.mbrc.events.ui.ScrobbleChange
import com.kelsos.mbrc.events.ui.ShuffleChange
import com.kelsos.mbrc.events.ui.VolumeChange
import com.kelsos.mbrc.interfaces.ICommand
import com.kelsos.mbrc.interfaces.IEvent
import com.kelsos.mbrc.model.MainDataModel
import javax.inject.Inject

class UpdatePlayerStatus
@Inject
constructor(
  private val model: MainDataModel,
  private val bus: RxBus
) : ICommand {

  override fun execute(e: IEvent) {
    val node = e.data as ObjectNode
    model.playState = node.path(Protocol.PlayerState).asText()

    val newMute = node.path(Protocol.PlayerMute).asBoolean()
    if (newMute != model.isMute) {
      model.isMute = newMute
      bus.post(if (newMute) VolumeChange() else VolumeChange(model.volume))
    }

    model.setRepeatState(node.path(Protocol.PlayerRepeat).asText())
    bus.post(RepeatChange(model.repeat))

    val newShuffle = node.path(Protocol.PlayerShuffle).asText()
    if (newShuffle != model.shuffle) {
      //noinspection ResourceType
      model.shuffle = newShuffle
      bus.post(ShuffleChange(newShuffle))
    }

    val newScrobbleState = node.path(Protocol.PlayerScrobble).asBoolean()
    if (newScrobbleState != model.isScrobblingEnabled) {
      model.isScrobblingEnabled = newScrobbleState
      bus.post(ScrobbleChange(newScrobbleState))
    }

    val newVolume = Integer.parseInt(node.path(Protocol.PlayerVolume).asText())
    if (newVolume != model.volume) {
      model.volume = newVolume
      bus.post(VolumeChange(model.volume))
    }
  }
}
