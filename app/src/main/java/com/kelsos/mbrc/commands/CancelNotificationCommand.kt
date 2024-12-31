package com.kelsos.mbrc.commands

import com.kelsos.mbrc.networking.protocol.ProtocolAction
import com.kelsos.mbrc.networking.protocol.ProtocolMessage
import com.kelsos.mbrc.platform.mediasession.SessionNotificationManager
import javax.inject.Inject

class CancelNotificationCommand
  @Inject
  constructor(
    private val sessionNotificationManager: SessionNotificationManager,
  ) : ProtocolAction {
    override fun execute(message: ProtocolMessage) {
      sessionNotificationManager.cancelNotification(SessionNotificationManager.NOW_PLAYING_PLACEHOLDER)
    }
  }
