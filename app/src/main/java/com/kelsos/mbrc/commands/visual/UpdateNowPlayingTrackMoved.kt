package com.kelsos.mbrc.commands.visual

import com.fasterxml.jackson.databind.node.ObjectNode
import com.kelsos.mbrc.events.bus.RxBus
import com.kelsos.mbrc.events.ui.TrackMoved
import com.kelsos.mbrc.interfaces.ICommand
import com.kelsos.mbrc.interfaces.IEvent
import javax.inject.Inject

class UpdateNowPlayingTrackMoved
  @Inject
  constructor(
    private val bus: RxBus,
  ) : ICommand {
    override fun execute(e: IEvent) {
      bus.post(TrackMoved(e.data as ObjectNode))
    }
  }
