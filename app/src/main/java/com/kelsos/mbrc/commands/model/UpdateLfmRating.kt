package com.kelsos.mbrc.commands.model

import com.kelsos.mbrc.events.bus.RxBus
import com.kelsos.mbrc.events.ui.LfmRatingChanged
import com.kelsos.mbrc.interfaces.ICommand
import com.kelsos.mbrc.interfaces.IEvent
import com.kelsos.mbrc.model.MainDataModel
import javax.inject.Inject

class UpdateLfmRating
@Inject
constructor(
  private val model: MainDataModel,
  private val bus: RxBus
) : ICommand {

  override fun execute(e: IEvent) {
    model.setLfmRating(e.dataString)
    bus.post(LfmRatingChanged(model.lfmStatus))
  }
}
