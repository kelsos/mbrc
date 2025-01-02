package com.kelsos.mbrc.networking.protocol

import android.app.Application
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Base64
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.node.BooleanNode
import com.fasterxml.jackson.databind.node.IntNode
import com.fasterxml.jackson.databind.node.ObjectNode
import com.fasterxml.jackson.databind.node.TextNode
import com.kelsos.mbrc.annotations.PlayerState
import com.kelsos.mbrc.common.state.MainDataModel
import com.kelsos.mbrc.common.utilities.AppCoroutineDispatchers
import com.kelsos.mbrc.constants.ProtocolEventType
import com.kelsos.mbrc.events.MessageEvent
import com.kelsos.mbrc.events.bus.RxBus
import com.kelsos.mbrc.events.ui.CoverChangedEvent
import com.kelsos.mbrc.events.ui.LfmRatingChanged
import com.kelsos.mbrc.events.ui.PlayStateChange
import com.kelsos.mbrc.events.ui.RatingChanged
import com.kelsos.mbrc.events.ui.RemoteClientMetaData
import com.kelsos.mbrc.events.ui.RepeatChange
import com.kelsos.mbrc.events.ui.ScrobbleChange
import com.kelsos.mbrc.events.ui.ShuffleChange
import com.kelsos.mbrc.events.ui.TrackInfoChangeEvent
import com.kelsos.mbrc.events.ui.TrackMoved
import com.kelsos.mbrc.events.ui.TrackRemoval
import com.kelsos.mbrc.events.ui.UpdateDuration
import com.kelsos.mbrc.events.ui.VolumeChange
import com.kelsos.mbrc.extensions.md5
import com.kelsos.mbrc.features.lyrics.LyricsModel
import com.kelsos.mbrc.features.lyrics.LyricsPayload
import com.kelsos.mbrc.features.player.CoverPayload
import com.kelsos.mbrc.features.player.ModelCache
import com.kelsos.mbrc.features.player.TrackInfo
import com.kelsos.mbrc.features.widgets.WidgetUpdater
import com.kelsos.mbrc.networking.ApiBase
import com.kelsos.mbrc.networking.SocketActivityChecker
import com.kelsos.mbrc.networking.client.SocketMessage
import com.kelsos.mbrc.networking.client.SocketService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import timber.log.Timber
import java.io.File
import java.io.FileOutputStream

class UpdateLastFm(
  private val model: MainDataModel,
  private val bus: RxBus,
) : ProtocolAction {
  override fun execute(message: ProtocolMessage) {
    val newScrobbleState = (message.data as BooleanNode).asBoolean()
    if (newScrobbleState != model.isScrobblingEnabled) {
      model.isScrobblingEnabled = newScrobbleState
      bus.post(ScrobbleChange(newScrobbleState))
    }
  }
}

class UpdateLfmRating(
  private val model: MainDataModel,
  private val bus: RxBus,
) : ProtocolAction {
  override fun execute(message: ProtocolMessage) {
    model.setLfmRating(message.dataString)
    bus.post(LfmRatingChanged(model.lfmStatus))
  }
}

class UpdateLyrics(
  private val model: LyricsModel,
  private val mapper: ObjectMapper,
) : ProtocolAction {
  override fun execute(message: ProtocolMessage) {
    val payload = mapper.treeToValue((message.data as JsonNode), LyricsPayload::class.java)

    model.status = payload.status
    if (payload.status == LyricsPayload.Companion.SUCCESS) {
      model.lyrics = payload.lyrics
    } else {
      model.lyrics = ""
    }
  }
}

class UpdateMute(
  private val model: MainDataModel,
  private val bus: RxBus,
) : ProtocolAction {
  override fun execute(message: ProtocolMessage) {
    val newMute = (message.data as BooleanNode).asBoolean()
    if (newMute != model.isMute) {
      model.isMute = newMute
      bus.post(if (newMute) VolumeChange() else VolumeChange(model.volume))
    }
  }
}

class UpdateNowPlayingTrack(
  private val model: MainDataModel,
  private val bus: RxBus,
  private val cache: ModelCache,
  private val widgetUpdater: WidgetUpdater,
  dispatchers: AppCoroutineDispatchers,
) : ProtocolAction {
  private val job = SupervisorJob()
  private val scope = CoroutineScope(job + dispatchers.io)

  override fun execute(message: ProtocolMessage) {
    val node = message.data as ObjectNode
    val artist = node.path("artist").textValue()
    val album = node.path("album").textValue()
    val title = node.path("title").textValue()
    val year = node.path("year").textValue()
    val path = node.path("path").textValue()
    model.trackInfo = TrackInfo(artist, title, album, year, path)
    save(model.trackInfo)
    bus.post(RemoteClientMetaData(model.trackInfo, model.coverPath, model.duration))
    bus.post(TrackInfoChangeEvent(model.trackInfo))
    widgetUpdater.updatePlayingTrack(model.trackInfo)
  }

  private fun save(info: TrackInfo) {
    scope.launch {
      try {
        cache.persistInfo(info)
        Timber.Forest.v("Playing track info successfully persisted")
      } catch (e: Exception) {
        Timber.Forest.v(e, "Failed to persist the playing track info")
      }
    }
  }
}

class UpdatePlayerStatus(
  private val model: MainDataModel,
  private val bus: RxBus,
) : ProtocolAction {
  override fun execute(message: ProtocolMessage) {
    val node = message.data as ObjectNode
    model.playState = node.path(Protocol.PLAYER_STATE).asText()

    val newMute = node.path(Protocol.PLAYER_MUTE).asBoolean()
    if (newMute != model.isMute) {
      model.isMute = newMute
      bus.post(if (newMute) VolumeChange() else VolumeChange(model.volume))
    }

    model.setRepeatState(node.path(Protocol.PLAYER_REPEAT).asText())
    bus.post(RepeatChange(model.repeat))

    val newShuffle = node.path(Protocol.PLAYER_SHUFFLE).asText()
    if (newShuffle != model.shuffle) {
      //noinspection ResourceType
      model.shuffle = newShuffle
      bus.post(ShuffleChange(newShuffle))
    }

    val newScrobbleState = node.path(Protocol.PLAYER_SCROBBLE).asBoolean()
    if (newScrobbleState != model.isScrobblingEnabled) {
      model.isScrobblingEnabled = newScrobbleState
      bus.post(ScrobbleChange(newScrobbleState))
    }

    val newVolume = Integer.parseInt(node.path(Protocol.PLAYER_VOLUME).asText())
    if (newVolume != model.volume) {
      model.volume = newVolume
      bus.post(VolumeChange(model.volume))
    }
  }
}

class UpdatePlayState(
  private val model: MainDataModel,
  private val bus: RxBus,
  private val widgetUpdater: WidgetUpdater,
  dispatchers: AppCoroutineDispatchers,
) : ProtocolAction {
  private val job = SupervisorJob()
  private val scope = CoroutineScope(job + dispatchers.io)
  private var action: Deferred<Unit>? = null

  override fun execute(message: ProtocolMessage) {
    model.playState = message.dataString
    if (model.playState != PlayerState.STOPPED) {
      bus.post(PlayStateChange(model.playState, model.position))
    } else {
      stop()
    }

    widgetUpdater.updatePlayState(message.dataString)
  }

  private fun stop() {
    action?.cancel()
    action =
      scope.async {
        delay(800)
        bus.post(PlayStateChange(model.playState, model.position))
      }
  }
}

class UpdatePluginVersionCommand(
  private val model: MainDataModel,
  private val bus: RxBus,
) : ProtocolAction {
  override fun execute(message: ProtocolMessage) {
    if (message.dataString.isEmpty()) {
      return
    }
    model.pluginVersion = message.dataString
    bus.post(MessageEvent(ProtocolEventType.PLUGIN_VERSION_CHECK))
  }
}

class UpdateRating(
  private val model: MainDataModel,
  private val bus: RxBus,
) : ProtocolAction {
  override fun execute(message: ProtocolMessage) {
    model.rating = (message.data as TextNode).asDouble(0.0).toFloat()
    bus.post(RatingChanged(model.rating))
  }
}

class UpdateRepeat(
  private val model: MainDataModel,
  private val bus: RxBus,
) : ProtocolAction {
  override fun execute(message: ProtocolMessage) {
    model.setRepeatState(message.dataString)
    bus.post(RepeatChange(model.repeat))
  }
}

class UpdateShuffle(
  private val model: MainDataModel,
  private val bus: RxBus,
) : ProtocolAction {
  override fun execute(message: ProtocolMessage) {
    var data: String? = message.dataString

    // Older plugin support, where the shuffle had boolean value.
    if (data == null) {
      data = if ((message.data as JsonNode).asBoolean()) ShuffleChange.SHUFFLE else ShuffleChange.OFF
    }

    if (data != model.shuffle) {
      //noinspection ResourceType
      model.shuffle = data
      bus.post(ShuffleChange(data))
    }
  }
}

class UpdateVolume(
  private val model: MainDataModel,
  private val bus: RxBus,
) : ProtocolAction {
  override fun execute(message: ProtocolMessage) {
    val newVolume = (message.data as IntNode).asInt()
    if (newVolume != model.volume) {
      model.volume = newVolume
      bus.post(VolumeChange(model.volume))
    }
  }
}

class UpdateCover(
  private val bus: RxBus,
  private val context: Application,
  private val mapper: ObjectMapper,
  private val coverService: ApiBase,
  private val model: MainDataModel,
  private val cache: ModelCache,
  private val widgetUpdater: WidgetUpdater,
  appCoroutineDispatchers: AppCoroutineDispatchers,
) : ProtocolAction {
  private val coverDir: File = File(context.filesDir, COVER_DIR)
  private val job = SupervisorJob()
  private val scope = CoroutineScope(job + appCoroutineDispatchers.io)

  override fun execute(message: ProtocolMessage) {
    val payload = mapper.treeToValue((message.data as JsonNode), CoverPayload::class.java)

    if (payload.status == CoverPayload.Companion.NOT_FOUND) {
      bus.post(CoverChangedEvent())
      widgetUpdater.updateCover()
    } else if (payload.status == CoverPayload.Companion.READY) {
      scope.launch {
        retrieveCover()
      }
    }
  }

  private suspend fun retrieveCover() {
    val (status, cover) =
      try {
        coverService.getItem(Protocol.NOW_PLAYING_COVER, CoverPayload::class)
      } catch (e: Exception) {
        return
      }

    if (status != CoverPayload.Companion.SUCCESS) {
      removeCover()
      return
    }

    try {
      val bitmap = getBitmap(cover)
      val file = storeCover(bitmap)
      val path = file.absolutePath
      model.coverPath = path
      savePath(path)
      bus.post(CoverChangedEvent(path))
      bus.post(RemoteClientMetaData(model.trackInfo, model.coverPath, model.duration))
      widgetUpdater.updateCover(path)
    } catch (e: Exception) {
      removeCover(e)
    }

    Timber.v("Message received for available cover")
    return
  }

  private fun savePath(path: String) {
    scope.launch {
      try {
        cache.persistCover(path)
        Timber.v("Playing track cover $path successfully persisted")
      } catch (e: Exception) {
        Timber.v(e, "Failed to persist the playing track cover")
      }
    }
  }

  private fun getBitmap(base64: String): Bitmap {
    val decodedImage = Base64.decode(base64, Base64.DEFAULT)
    val bitmap = BitmapFactory.decodeByteArray(decodedImage, 0, decodedImage.size)
    if (bitmap != null) {
      return bitmap
    } else {
      throw RuntimeException("Base64 was not an image")
    }
  }

  private fun removeCover(it: Throwable? = null) {
    clearPreviousCovers(0)

    it?.let {
      Timber.v(it, "Failed to store path")
    }

    bus.post(CoverChangedEvent())
  }

  private fun storeCover(bitmap: Bitmap): File {
    checkIfExists()
    clearPreviousCovers()
    val file = temporaryCover()
    val fileStream = FileOutputStream(file)
    val success = bitmap.compress(Bitmap.CompressFormat.JPEG, 100, fileStream)
    fileStream.close()
    if (success) {
      val md5 = file.md5()
      val extension = file.extension
      val newFile = File(coverDir, "$md5.$extension")
      file.renameTo(newFile)
      Timber.v("file was renamed to %s", newFile.absolutePath)
      return newFile
    } else {
      throw RuntimeException("unable to store cover")
    }
  }

  private fun checkIfExists() {
    if (!coverDir.exists()) {
      coverDir.mkdir()
    }
  }

  private fun clearPreviousCovers(keep: Int = 2) {
    coverDir.listFiles()?.run {
      sortByDescending(File::lastModified)
      Timber.v(size.toString())
      val toDelete = if (size - keep < 0) 0 else size - keep
      Timber.v("deleting last $toDelete items")
      takeLast(toDelete).forEach {
        it.delete()
      }
    }
  }

  private fun temporaryCover(): File {
    val file = File(context.cacheDir, TEMP_COVER)
    if (file.exists()) {
      file.delete()
    }
    return file
  }

  companion object {
    const val COVER_DIR = "cover"
    const val TEMP_COVER = "temp_cover.jpg"
  }
}

class ProtocolPingHandle(
  private val service: SocketService,
  private val activityChecker: SocketActivityChecker,
) : ProtocolAction {
  override fun execute(message: ProtocolMessage) {
    activityChecker.ping()
    service.sendData(SocketMessage.Companion.create(Protocol.PONG, ""))
  }
}

class SimpleLogCommand : ProtocolAction {
  override fun execute(message: ProtocolMessage) {
    Timber.d("handled message ${message.type}: ${message.data}")
  }
}

class UpdateNowPlayingTrackMoved(
  private val bus: RxBus,
) : ProtocolAction {
  override fun execute(message: ProtocolMessage) {
    bus.post(TrackMoved(message.data as ObjectNode))
  }
}

class UpdateNowPlayingTrackRemoval(
  private val bus: RxBus,
) : ProtocolAction {
  override fun execute(message: ProtocolMessage) {
    message.data
    bus.post(TrackRemoval(message.data as ObjectNode))
  }
}

class UpdatePlaybackPositionCommand(
  private val bus: RxBus,
  private val model: MainDataModel,
) : ProtocolAction {
  override fun execute(message: ProtocolMessage) {
    val data = message.data as ObjectNode
    val duration = data.path("total").asLong()
    val position = data.path("current").asLong()
    bus.post(UpdateDuration(position.toInt(), duration.toInt()))

    bus.post(RemoteClientMetaData(model.trackInfo, model.coverPath, duration))

    if (position != model.position) {
      bus.post(PlayStateChange(model.playState, position))
    }

    model.duration = duration
    model.position = position
  }
}
