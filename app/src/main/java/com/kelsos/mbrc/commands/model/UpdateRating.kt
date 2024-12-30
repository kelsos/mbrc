package com.kelsos.mbrc.commands.model

import com.fasterxml.jackson.databind.node.TextNode
import com.kelsos.mbrc.events.bus.RxBus
import com.kelsos.mbrc.events.ui.RatingChanged
import com.kelsos.mbrc.interfaces.ICommand
import com.kelsos.mbrc.interfaces.IEvent
import com.kelsos.mbrc.model.MainDataModel
import javax.inject.Inject

class UpdateRating
  @Inject
  constructor(
    private val model: MainDataModel,
    private val bus: RxBus,
  ) : ICommand {
    override fun execute(e: IEvent) {
      model.rating = (e.data as TextNode).asDouble(0.0).toFloat()
      bus.post(RatingChanged(model.rating))
    }
  }
