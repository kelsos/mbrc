package com.kelsos.mbrc.commands

import com.kelsos.mbrc.interfaces.ICommand
import com.kelsos.mbrc.interfaces.IEvent
import com.kelsos.mbrc.model.ConnectionModel
import com.kelsos.mbrc.services.ProtocolHandler
import javax.inject.Inject

class HandleHandshake
@Inject constructor(private val handler: ProtocolHandler, private val model: ConnectionModel) :
  ICommand {

  override fun execute(e: IEvent) {
    if (!(e.data as Boolean)) {
      handler.resetHandshake()
      model.setHandShakeDone(false)
    }
  }
}
