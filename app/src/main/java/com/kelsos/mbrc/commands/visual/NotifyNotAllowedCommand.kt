package com.kelsos.mbrc.commands.visual

import com.kelsos.mbrc.R
import com.kelsos.mbrc.annotations.SocketAction.STOP
import com.kelsos.mbrc.events.bus.RxBus
import com.kelsos.mbrc.events.ui.NotifyUser
import com.kelsos.mbrc.interfaces.ICommand
import com.kelsos.mbrc.interfaces.IEvent
import com.kelsos.mbrc.model.ConnectionModel
import com.kelsos.mbrc.services.ProtocolHandler
import com.kelsos.mbrc.services.SocketService
import javax.inject.Inject

class NotifyNotAllowedCommand
  @Inject
  constructor(
    private val socketService: SocketService,
    private val model: ConnectionModel,
    private val handler: ProtocolHandler,
    private val bus: RxBus,
  ) : ICommand {
    override fun execute(e: IEvent) {
      bus.post(NotifyUser(R.string.notification_not_allowed))
      socketService.socketManager(STOP)
      model.setConnectionState("false")
      handler.resetHandshake()
    }
  }
