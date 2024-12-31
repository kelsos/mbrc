package com.kelsos.mbrc.commands.visual

import com.kelsos.mbrc.common.state.ConnectionModel
import com.kelsos.mbrc.common.state.MainDataModel
import com.kelsos.mbrc.features.library.LibrarySyncInteractor
import com.kelsos.mbrc.networking.client.SocketMessage
import com.kelsos.mbrc.networking.client.SocketService
import com.kelsos.mbrc.networking.protocol.Protocol
import com.kelsos.mbrc.networking.protocol.ProtocolAction
import com.kelsos.mbrc.networking.protocol.ProtocolMessage
import rx.Observable
import timber.log.Timber
import java.util.ArrayList
import java.util.concurrent.TimeUnit
import javax.inject.Inject

class HandshakeCompletionActions
  @Inject
  constructor(
    private val service: SocketService,
    private val model: MainDataModel,
    private val connectionModel: ConnectionModel,
    private val syncInteractor: LibrarySyncInteractor,
  ) : ProtocolAction {
    override fun execute(message: ProtocolMessage) {
      val isComplete = message.data as Boolean
      connectionModel.setHandShakeDone(isComplete)

      if (!isComplete) {
        return
      }

      if (model.pluginProtocol > 2) {
        Timber.v("Sending init request")
        service.sendData(SocketMessage.create(Protocol.INIT))
        service.sendData(SocketMessage.create(Protocol.PLUGIN_VERSION))
      } else {
        Timber.v("Preparing to send requests for state")

        val messages = ArrayList<SocketMessage>()
        messages.add(SocketMessage.create(Protocol.NOW_PLAYING_COVER))
        messages.add(SocketMessage.create(Protocol.PLAYER_STATUS))
        messages.add(SocketMessage.create(Protocol.NOW_PLAYING_TRACK))
        messages.add(SocketMessage.create(Protocol.NOW_PLAYING_LYRICS))
        messages.add(SocketMessage.create(Protocol.NOW_PLAYING_POSITION))
        messages.add(SocketMessage.create(Protocol.PLUGIN_VERSION))

        val totalMessages = messages.size
        Observable
          .interval(150, TimeUnit.MILLISECONDS)
          .take(totalMessages)
          .subscribe({ service.sendData(messages.removeAt(0)) }) {
            Timber.v(it, "Failure while sending the init messages")
          }
      }

      syncInteractor.sync(true)
    }
  }
