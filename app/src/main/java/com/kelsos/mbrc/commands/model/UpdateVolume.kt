package com.kelsos.mbrc.commands.model

import com.fasterxml.jackson.databind.node.IntNode
import com.kelsos.mbrc.events.bus.RxBus
import com.kelsos.mbrc.events.ui.VolumeChange
import com.kelsos.mbrc.interfaces.ICommand
import com.kelsos.mbrc.interfaces.IEvent
import com.kelsos.mbrc.model.MainDataModel
import javax.inject.Inject

class UpdateVolume
  @Inject
  constructor(
    private val model: MainDataModel,
    private val bus: RxBus,
  ) : ICommand {
    override fun execute(e: IEvent) {
      val newVolume = (e.data as IntNode).asInt()
      if (newVolume != model.volume) {
        model.volume = newVolume
        bus.post(VolumeChange(model.volume))
      }
    }
  }
