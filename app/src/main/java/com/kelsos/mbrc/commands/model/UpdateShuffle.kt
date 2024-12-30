package com.kelsos.mbrc.commands.model

import com.fasterxml.jackson.databind.JsonNode
import com.kelsos.mbrc.events.bus.RxBus
import com.kelsos.mbrc.events.ui.ShuffleChange
import com.kelsos.mbrc.interfaces.ICommand
import com.kelsos.mbrc.interfaces.IEvent
import com.kelsos.mbrc.model.MainDataModel
import javax.inject.Inject

class UpdateShuffle
@Inject
constructor(
  private val model: MainDataModel,
  private val bus: RxBus
) : ICommand {

  override fun execute(e: IEvent) {
    var data: String? = e.dataString

    // Older plugin support, where the shuffle had boolean value.
    if (data == null) {
      data = if ((e.data as JsonNode).asBoolean()) ShuffleChange.SHUFFLE else ShuffleChange.OFF
    }

    if (data != model.shuffle) {
      //noinspection ResourceType
      model.shuffle = data
      bus.post(ShuffleChange(data))
    }
  }
}
