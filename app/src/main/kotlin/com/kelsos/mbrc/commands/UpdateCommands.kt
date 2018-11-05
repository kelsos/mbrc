package com.kelsos.mbrc.commands

import android.app.Application
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.node.BooleanNode
import com.fasterxml.jackson.databind.node.IntNode
import com.fasterxml.jackson.databind.node.ObjectNode
import com.fasterxml.jackson.databind.node.TextNode
import com.kelsos.mbrc.constants.Protocol
import com.kelsos.mbrc.data.LyricsPayload
import com.kelsos.mbrc.domain.TrackInfo
import com.kelsos.mbrc.events.bus.RxBus
import com.kelsos.mbrc.events.ui.RemoteClientMetaData
import com.kelsos.mbrc.events.ui.ShuffleChange
import com.kelsos.mbrc.events.ui.TrackInfoChangeEvent
import com.kelsos.mbrc.interfaces.ICommand
import com.kelsos.mbrc.interfaces.IEvent
import com.kelsos.mbrc.model.LyricsModel
import com.kelsos.mbrc.model.MainDataModel
import com.kelsos.mbrc.widgets.UpdateWidgets
import javax.inject.Inject

class UpdateLastFm
@Inject constructor(private val model: MainDataModel) : ICommand {

  override fun execute(e: IEvent) {
    model.isScrobblingEnabled = (e.data as BooleanNode).asBoolean()
  }
}

class UpdateLfmRating
@Inject constructor(private val model: MainDataModel) : ICommand {

  override fun execute(e: IEvent) {
    model.setLfmRating(e.dataString)
  }
}

class UpdateLyrics
@Inject
constructor(
    private val model: LyricsModel,
    private val mapper: ObjectMapper
) : ICommand {

  override fun execute(e: IEvent) {
    val payload = mapper.treeToValue((e.data as JsonNode), LyricsPayload::class.java)

    model.status = payload.status
    if (payload.status == LyricsPayload.SUCCESS) {
      model.lyrics = payload.lyrics
    } else {
      model.lyrics = ""
    }
  }
}

class UpdateMute
@Inject constructor(private val model: MainDataModel) : ICommand {

  override fun execute(e: IEvent) {
    model.isMute = (e.data as BooleanNode).asBoolean()
  }
}

class UpdateNowPlayingTrack
@Inject constructor(
    private val model: MainDataModel,
    private val context: Application,
    private val bus: RxBus
) : ICommand {

  override fun execute(e: IEvent) {
    val node = e.data as ObjectNode
    val artist = node.path("artist").textValue()
    val album = node.path("album").textValue()
    val title = node.path("title").textValue()
    val year = node.path("year").textValue()
    val path = node.path("path").textValue()
    model.trackInfo = TrackInfo(artist, title, album, year, path)
    bus.post(RemoteClientMetaData(model.trackInfo, model.coverPath))
    bus.post(TrackInfoChangeEvent(model.trackInfo))
    UpdateWidgets.updateTrackInfo(context, model.trackInfo)
  }
}

class UpdatePlayerStatus
@Inject constructor(private val model: MainDataModel) : ICommand {

  override fun execute(e: IEvent) {
    val node = e.data as ObjectNode
    model.playState = node.path(Protocol.PlayerState).asText()
    model.isMute = node.path(Protocol.PlayerMute).asBoolean()
    model.setRepeatState(node.path(Protocol.PlayerRepeat).asText())
    //noinspection ResourceType
    model.shuffle = node.path(Protocol.PlayerShuffle).asText()
    model.isScrobblingEnabled = node.path(Protocol.PlayerScrobble).asBoolean()
    model.volume = Integer.parseInt(node.path(Protocol.PlayerVolume).asText())
  }
}

class UpdatePlayState
@Inject constructor(
    private val model: MainDataModel,
    private val context: Application
) : ICommand {

  override fun execute(e: IEvent) {
    model.playState = e.dataString
    UpdateWidgets.updatePlaystate(context, e.dataString)
  }
}

class UpdatePluginVersionCommand
@Inject constructor(private val model: MainDataModel) : ICommand {

  override fun execute(e: IEvent) {
    model.pluginVersion = e.dataString
  }
}

class UpdateRating
@Inject constructor(private val model: MainDataModel) : ICommand {

  override fun execute(e: IEvent) {
    model.rating = (e.data as TextNode).asDouble(0.0).toFloat()
  }
}

class UpdateRepeat
@Inject constructor(private val model: MainDataModel) : ICommand {

  override fun execute(e: IEvent) {
    model.setRepeatState(e.dataString)
  }
}

class UpdateShuffle
@Inject constructor(private val model: MainDataModel) : ICommand {

  override fun execute(e: IEvent) {
    var data: String? = e.dataString

    // Older plugin support, where the shuffle had boolean value.
    if (data == null) {
      data = if ((e.data as JsonNode).asBoolean()) {
        ShuffleChange.SHUFFLE
      } else {
        ShuffleChange.OFF
      }
    }

    //noinspection ResourceType
    model.shuffle = data
  }
}

class UpdateVolume
@Inject constructor(private val model: MainDataModel) : ICommand {

  override fun execute(e: IEvent) {
    model.volume = (e.data as IntNode).asInt()
  }
}
