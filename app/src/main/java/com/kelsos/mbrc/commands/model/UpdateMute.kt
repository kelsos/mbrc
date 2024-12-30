package com.kelsos.mbrc.commands.model

import com.fasterxml.jackson.databind.node.BooleanNode
import com.kelsos.mbrc.events.bus.RxBus
import com.kelsos.mbrc.events.ui.VolumeChange
import com.kelsos.mbrc.interfaces.ICommand
import com.kelsos.mbrc.interfaces.IEvent
import com.kelsos.mbrc.model.MainDataModel
import javax.inject.Inject

class UpdateMute
@Inject
constructor(
  private val model: MainDataModel,
  private val bus: RxBus
) : ICommand {

  override fun execute(e: IEvent) {
    val newMute = (e.data as BooleanNode).asBoolean()
    if (newMute != model.isMute) {
      model.isMute = newMute
      bus.post(if (newMute) VolumeChange() else VolumeChange(model.volume))
    }
  }
}
