package com.kelsos.mbrc.commands.model

import com.kelsos.mbrc.constants.ProtocolEventType
import com.kelsos.mbrc.events.MessageEvent
import com.kelsos.mbrc.events.bus.RxBus
import com.kelsos.mbrc.interfaces.ICommand
import com.kelsos.mbrc.interfaces.IEvent
import com.kelsos.mbrc.model.MainDataModel
import javax.inject.Inject

class UpdatePluginVersionCommand
  @Inject
  constructor(
    private val model: MainDataModel,
    private val bus: RxBus,
  ) : ICommand {
    override fun execute(e: IEvent) {
      if (e.dataString.isEmpty()) {
        return
      }
      model.pluginVersion = e.dataString
      bus.post(MessageEvent(ProtocolEventType.PluginVersionCheck))
    }
  }
