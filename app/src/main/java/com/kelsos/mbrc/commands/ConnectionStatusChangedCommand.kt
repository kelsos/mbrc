package com.kelsos.mbrc.commands

import com.kelsos.mbrc.constants.Protocol
import com.kelsos.mbrc.data.SocketMessage
import com.kelsos.mbrc.interfaces.ICommand
import com.kelsos.mbrc.interfaces.IEvent
import com.kelsos.mbrc.messaging.NotificationService
import com.kelsos.mbrc.model.ConnectionModel
import com.kelsos.mbrc.services.SocketService
import javax.inject.Inject

class ConnectionStatusChangedCommand
  @Inject
  constructor(
    private val model: ConnectionModel,
    private val service: SocketService,
    private val notificationService: NotificationService,
  ) : ICommand {
    override fun execute(e: IEvent) {
      model.setConnectionState(e.dataString)

      if (model.isConnectionActive) {
        service.sendData(SocketMessage.create(Protocol.Player, "Android"))
      } else {
        notificationService.cancelNotification(NotificationService.NOW_PLAYING_PLACEHOLDER)
      }
    }
  }
