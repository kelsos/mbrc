package com.kelsos.mbrc.commands.model

import com.fasterxml.jackson.databind.node.BooleanNode
import com.kelsos.mbrc.events.bus.RxBus
import com.kelsos.mbrc.events.ui.ScrobbleChange
import com.kelsos.mbrc.interfaces.ICommand
import com.kelsos.mbrc.interfaces.IEvent
import com.kelsos.mbrc.model.MainDataModel
import javax.inject.Inject

class UpdateLastFm
@Inject
constructor(
  private val model: MainDataModel,
  private val bus: RxBus
) : ICommand {

  override fun execute(e: IEvent) {
    val newScrobbleState = (e.data as BooleanNode).asBoolean()
    if (newScrobbleState != model.isScrobblingEnabled) {
      model.isScrobblingEnabled = newScrobbleState
      bus.post(ScrobbleChange(newScrobbleState))
    }
  }
}
