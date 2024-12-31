package com.kelsos.mbrc.commands

import com.kelsos.mbrc.common.state.ConnectionModel
import com.kelsos.mbrc.networking.client.SocketMessage
import com.kelsos.mbrc.networking.client.SocketService
import com.kelsos.mbrc.networking.protocol.Protocol
import com.kelsos.mbrc.networking.protocol.ProtocolAction
import com.kelsos.mbrc.networking.protocol.ProtocolMessage
import com.kelsos.mbrc.platform.mediasession.SessionNotificationManager
import javax.inject.Inject

class ConnectionStatusChangedCommand
  @Inject
  constructor(
    private val model: ConnectionModel,
    private val service: SocketService,
    private val sessionNotificationManager: SessionNotificationManager,
  ) : ProtocolAction {
    override fun execute(message: ProtocolMessage) {
      model.setConnectionState(message.dataString)

      if (model.isConnectionActive) {
        service.sendData(SocketMessage.create(Protocol.PLAYER, "Android"))
      } else {
        sessionNotificationManager.cancelNotification(SessionNotificationManager.NOW_PLAYING_PLACEHOLDER)
      }
    }
  }
