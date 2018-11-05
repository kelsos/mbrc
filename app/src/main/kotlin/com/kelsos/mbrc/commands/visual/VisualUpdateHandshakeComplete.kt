package com.kelsos.mbrc.commands.visual

import com.kelsos.mbrc.constants.Protocol
import com.kelsos.mbrc.data.SocketMessage
import com.kelsos.mbrc.interfaces.ICommand
import com.kelsos.mbrc.interfaces.IEvent
import com.kelsos.mbrc.model.ConnectionModel
import com.kelsos.mbrc.model.MainDataModel
import com.kelsos.mbrc.services.SocketService
import rx.Observable
import timber.log.Timber
import java.util.*
import java.util.concurrent.TimeUnit
import javax.inject.Inject

class VisualUpdateHandshakeComplete
@Inject constructor(private val service: SocketService, private val model: MainDataModel, private val connectionModel: ConnectionModel) : ICommand {

  override fun execute(e: IEvent) {
    val isComplete = e.data as Boolean
    connectionModel.setHandShakeDone(isComplete)

    if (!isComplete) {
      return
    }

    if (model.pluginProtocol > 2) {
      Timber.v("Sending init request")
      service.sendData(SocketMessage.create(Protocol.INIT))
    } else {

      Timber.v("Preparing to send requests for state")

      val messages = ArrayList<SocketMessage>()
      messages.add(SocketMessage.create(Protocol.NowPlayingCover))
      messages.add(SocketMessage.create(Protocol.PlayerStatus))
      messages.add(SocketMessage.create(Protocol.NowPlayingTrack))
      messages.add(SocketMessage.create(Protocol.NowPlayingLyrics))
      messages.add(SocketMessage.create(Protocol.NowPlayingPosition))
      messages.add(SocketMessage.create(Protocol.PluginVersion))

      val totalMessages = messages.size
      Observable.interval(150, TimeUnit.MILLISECONDS).take(totalMessages).subscribe { tick -> service.sendData(messages.removeAt(0)) }
    }
  }
}


