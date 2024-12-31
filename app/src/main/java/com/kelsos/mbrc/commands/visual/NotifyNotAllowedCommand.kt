package com.kelsos.mbrc.commands.visual

import com.kelsos.mbrc.R
import com.kelsos.mbrc.annotations.SocketAction.STOP
import com.kelsos.mbrc.common.state.ConnectionModel
import com.kelsos.mbrc.events.bus.RxBus
import com.kelsos.mbrc.events.ui.NotifyUser
import com.kelsos.mbrc.networking.client.SocketService
import com.kelsos.mbrc.networking.protocol.ProtocolAction
import com.kelsos.mbrc.networking.protocol.ProtocolHandler
import com.kelsos.mbrc.networking.protocol.ProtocolMessage
import javax.inject.Inject

class NotifyNotAllowedCommand
  @Inject
  constructor(
    private val socketService: SocketService,
    private val model: ConnectionModel,
    private val handler: ProtocolHandler,
    private val bus: RxBus,
  ) : ProtocolAction {
    override fun execute(message: ProtocolMessage) {
      bus.post(NotifyUser(R.string.notification_not_allowed))
      socketService.socketManager(STOP)
      model.setConnectionState("false")
      handler.resetHandshake()
    }
  }
