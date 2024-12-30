package com.kelsos.mbrc.commands.visual

import com.fasterxml.jackson.databind.node.ObjectNode
import com.kelsos.mbrc.events.bus.RxBus
import com.kelsos.mbrc.events.ui.PlayStateChange
import com.kelsos.mbrc.events.ui.RemoteClientMetaData
import com.kelsos.mbrc.events.ui.UpdateDuration
import com.kelsos.mbrc.interfaces.ICommand
import com.kelsos.mbrc.interfaces.IEvent
import com.kelsos.mbrc.model.MainDataModel
import javax.inject.Inject

class UpdatePlaybackPositionCommand
@Inject
constructor(
  private val bus: RxBus,
  private val model: MainDataModel,
) : ICommand {

  override fun execute(e: IEvent) {
    val data = e.data as ObjectNode
    val duration = data.path("total").asLong()
    val position = data.path("current").asLong()
    bus.post(UpdateDuration(position.toInt(), duration.toInt()))
    
    bus.post(RemoteClientMetaData(model.trackInfo, model.coverPath, duration))

    if (position != model.position) {
      bus.post(PlayStateChange(model.playState, position))
    }

    model.duration = duration
    model.position = position
  }
}
