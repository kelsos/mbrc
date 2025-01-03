package com.kelsos.mbrc.networking.protocol

import android.app.Application
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Base64
import androidx.core.net.toUri
import com.kelsos.mbrc.common.state.AppStatePublisher
import com.kelsos.mbrc.common.state.LfmRating
import com.kelsos.mbrc.common.state.NowPlayingTrack
import com.kelsos.mbrc.common.state.PlayerState
import com.kelsos.mbrc.common.state.PlayerStatus
import com.kelsos.mbrc.common.state.PlayerStatusModel
import com.kelsos.mbrc.common.state.PlayingPosition
import com.kelsos.mbrc.common.state.PlayingTrack
import com.kelsos.mbrc.common.state.PlayingTrackCache
import com.kelsos.mbrc.common.state.Repeat
import com.kelsos.mbrc.common.state.ShuffleMode
import com.kelsos.mbrc.common.state.TrackRating
import com.kelsos.mbrc.common.utilities.AppCoroutineDispatchers
import com.kelsos.mbrc.features.lyrics.LyricsPayload
import com.kelsos.mbrc.features.nowplaying.NowPlayingRepository
import com.kelsos.mbrc.features.player.CoverPayload
import com.kelsos.mbrc.features.widgets.WidgetUpdater
import com.kelsos.mbrc.networking.ApiBase
import com.kelsos.mbrc.networking.SocketActivityChecker
import com.kelsos.mbrc.networking.client.MessageQueue
import com.kelsos.mbrc.networking.client.PluginUpdateCheckUseCase
import com.kelsos.mbrc.networking.client.SocketMessage
import com.squareup.moshi.Moshi
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okio.HashingSink
import okio.blackholeSink
import okio.buffer
import okio.source
import timber.log.Timber
import java.io.File
import java.io.FileOutputStream

class UpdateLastFm(
  private val appState: AppStatePublisher,
) : ProtocolAction {
  override suspend fun execute(message: ProtocolMessage) {
    val previousState = appState.playerStatus.firstOrNull() ?: PlayerStatusModel()
    appState.updatePlayerStatus(previousState.copy(scrobbling = message.asBoolean()))
  }
}

class UpdateLfmRating(
  private val appState: AppStatePublisher,
) : ProtocolAction {
  override suspend fun execute(message: ProtocolMessage) {
    val previousState = appState.playingTrackRating.firstOrNull() ?: TrackRating()
    val lfmRating = LfmRating.fromString(message.data as? String)
    appState.updateTrackRating(previousState.copy(lfmRating = lfmRating))
  }
}

class UpdateLyrics(
  private val mapper: Moshi,
  private val appState: AppStatePublisher,
) : ProtocolAction {
  override suspend fun execute(message: ProtocolMessage) {
    val adapter = mapper.adapter(LyricsPayload::class.java)
    val payload = adapter.fromJsonValue(message.data) ?: return

    val lyrics =
      if (payload.status == LyricsPayload.SUCCESS) {
        payload.lyrics
          .replace("<p>", "\r\n")
          .replace("<br>", "\n")
          .replace("&lt;", "<")
          .replace("&gt;", ">")
          .replace("&quot;", "\"")
          .replace("&apos;", "'")
          .replace("&amp;", "&")
          .trim()
          .split(LYRICS_NEWLINE.toRegex())
          .dropLastWhile(String::isEmpty)
      } else {
        emptyList()
      }

    appState.updateLyrics(lyrics)
  }

  companion object {
    private const val LYRICS_NEWLINE = "\r\n|\n"
  }
}

class UpdateMute(
  private val appState: AppStatePublisher,
) : ProtocolAction {
  override suspend fun execute(message: ProtocolMessage) {
    val previousState = appState.playerStatus.firstOrNull() ?: PlayerStatusModel()
    appState.updatePlayerStatus(previousState.copy(mute = message.asBoolean()))
  }
}

class UpdateNowPlayingTrack(
  private val appState: AppStatePublisher,
  private val updater: WidgetUpdater,
  private val mapper: Moshi,
  private val cache: PlayingTrackCache,
) : ProtocolAction {
  override suspend fun execute(message: ProtocolMessage) {
    val adapter = mapper.adapter(NowPlayingTrack::class.java)
    val track = adapter.fromJsonValue(message.data) ?: return
    val previousState = appState.playingTrack.firstOrNull() ?: PlayingTrack()
    val newState =
      previousState.copy(
        artist = track.artist,
        title = track.title,
        album = track.album,
        year = track.year,
        path = track.path,
      )
    appState.updatePlayingTrack(newState)
    updater.updatePlayingTrack(newState)
    cache.persistInfo(newState)
  }
}

class UpdatePlayerStatus(
  private val appState: AppStatePublisher,
  private val moshi: Moshi,
) : ProtocolAction {
  override suspend fun execute(message: ProtocolMessage) {
    val adapter = moshi.adapter(PlayerStatus::class.java)
    val status = adapter.fromJsonValue(message.data) ?: return
    val previousState = appState.playerStatus.firstOrNull() ?: PlayerStatusModel()
    appState.updatePlayerStatus(
      previousState.copy(
        mute = status.mute,
        state = PlayerState.fromString(status.playState),
        repeat = Repeat.fromString(status.repeat),
        shuffle = ShuffleMode.fromString(status.shuffle),
        scrobbling = status.scrobbling,
        volume = status.volume,
      ),
    )
  }
}

class UpdatePlayState(
  private val appState: AppStatePublisher,
  private val updater: WidgetUpdater,
) : ProtocolAction {
  override suspend fun execute(message: ProtocolMessage) {
    val playState = PlayerState.fromString(message.data as? String)
    val previousState = appState.playerStatus.firstOrNull() ?: PlayerStatusModel()
    appState.updatePlayerStatus(previousState.copy(state = playState))
    updater.updatePlayState(playState)
  }
}

class UpdatePluginVersionCommand(
  private val pluginUpdateCheck: PluginUpdateCheckUseCase,
) : ProtocolAction {
  override suspend fun execute(message: ProtocolMessage) {
    val pluginVersion = message.data as? String
    Timber.v("plugin reports $pluginVersion")
    pluginVersion?.let { version ->
      pluginUpdateCheck.checkIfUpdateNeeded(version)
    }
  }
}

class UpdateRating(
  private val appState: AppStatePublisher,
) : ProtocolAction {
  override suspend fun execute(message: ProtocolMessage) {
    val rating = message.data.toString().toFloatOrNull()
    val previousState = appState.playingTrackRating.firstOrNull() ?: TrackRating()
    appState.updateTrackRating(previousState.copy(rating = rating ?: 0.0f))
  }
}

class UpdateRepeat(
  private val appState: AppStatePublisher,
) : ProtocolAction {
  override suspend fun execute(message: ProtocolMessage) {
    val repeat = Repeat.fromString(message.data as? String)
    val previousState = appState.playerStatus.firstOrNull() ?: PlayerStatusModel()
    appState.updatePlayerStatus(previousState.copy(repeat = repeat))
  }
}

class UpdateShuffle(
  private val appState: AppStatePublisher,
) : ProtocolAction {
  override suspend fun execute(message: ProtocolMessage) {
    val data = ShuffleMode.fromString(message.data as? String)
    val previousState = appState.playerStatus.firstOrNull() ?: PlayerStatusModel()
    appState.updatePlayerStatus(previousState.copy(shuffle = data))
  }
}

class UpdateVolume(
  private val appState: AppStatePublisher,
) : ProtocolAction {
  override suspend fun execute(message: ProtocolMessage) {
    val volume = message.data as Number
    val previousState = appState.playerStatus.firstOrNull() ?: PlayerStatusModel()
    appState.updatePlayerStatus(previousState.copy(volume = volume.toInt()))
  }
}

class UpdateCover(
  private val app: Application,
  private val updater: WidgetUpdater,
  private val moshi: Moshi,
  private val api: ApiBase,
  private val dispatchers: AppCoroutineDispatchers,
  private val appState: AppStatePublisher,
) : ProtocolAction {
  private val coverDir: File = File(app.filesDir, COVER_DIR)

  override suspend fun execute(message: ProtocolMessage) {
    val adapter = moshi.adapter(CoverPayload::class.java)
    val payload = adapter.fromJsonValue(message.data) ?: return
    val previousState = appState.playingTrack.firstOrNull() ?: PlayingTrack()
    if (payload.status == CoverPayload.NOT_FOUND) {
      update(previousState)
    } else if (payload.status == CoverPayload.READY) {
      retrieveCover(previousState)
    }
  }

  private suspend fun retrieveCover(previousState: PlayingTrack) {
    withContext(dispatchers.network) {
      val result =
        runCatching {
          val response = api.getItem(Protocol.NowPlayingCover, CoverPayload::class)
          val bitmap = getBitmap(response.cover)
          val file = storeCover(bitmap)

          val coverUri = file.toUri().toString()
          update(previousState, coverUri)
        }

      if (result.isFailure) {
        removeCover(result.exceptionOrNull(), previousState)
      }
    }

    Timber.v("Message received for available cover")
    return
  }

  private suspend fun update(
    previousState: PlayingTrack,
    coverUri: String = "",
  ) {
    val newState = previousState.copy(coverUrl = coverUri)
    appState.updatePlayingTrack(newState)
    updater.updateCover(coverUri)
  }

  private fun getBitmap(base64: String): Bitmap {
    val decodedImage = Base64.decode(base64, Base64.DEFAULT)
    val bitmap = BitmapFactory.decodeByteArray(decodedImage, 0, decodedImage.size)
    return checkNotNull(bitmap) { "Base64 was not an image" }
  }

  private suspend fun removeCover(
    it: Throwable? = null,
    previousState: PlayingTrack,
  ) {
    clearPreviousCovers(0)

    it?.let {
      Timber.v(it, "Failed to store path")
    }

    update(previousState)
  }

  private fun storeCover(bitmap: Bitmap): File {
    checkIfExists()
    clearPreviousCovers()

    val file = temporaryCover()
    val fileStream = FileOutputStream(file)
    val success = bitmap.compress(Bitmap.CompressFormat.JPEG, JPEG_QUALITY, fileStream)
    fileStream.close()

    val md5 =
      HashingSink.md5(blackholeSink()).use { hashingSink ->
        file.source().buffer().use { source ->
          source.readAll(hashingSink)
          hashingSink.hash.md5().hex()
        }
      }

    val extension = file.extension
    val newFile = File(app.filesDir, "$md5.$extension")
    if (newFile.exists()) {
      val isDeleted = file.delete()
      if (!isDeleted) {
        Timber.v("unable to delete temporary cover ${file.absolutePath}")
      }
      return newFile
    }

    check(success) { "unable to store cover" }
    val isRenamed = file.renameTo(newFile)
    Timber.v("file was renamed $isRenamed to ${newFile.absolutePath}")
    return newFile
  }

  private fun checkIfExists() {
    if (!coverDir.exists()) {
      val isSuccessful = coverDir.mkdir()
      Timber.v("cover directory ${coverDir.absolutePath} creation is successful $isSuccessful")
    }
  }

  private fun clearPreviousCovers(keep: Int = 1) {
    if (!coverDir.exists()) {
      return
    }
    val storedCovers = coverDir.listFiles() ?: return
    storedCovers.sortByDescending(File::lastModified)
    val elementsToKeep = if (storedCovers.size - keep < 0) 0 else storedCovers.size - keep
    storedCovers.takeLast(elementsToKeep).forEach {
      val isSuccessful = it.delete()
      if (!isSuccessful) {
        Timber.v("unable to cover delete ${it.absolutePath}")
      }
    }
  }

  private fun temporaryCover(): File {
    val file = File(app.cacheDir, TEMP_COVER)
    if (file.exists()) {
      val isSuccessful = file.delete()
      if (!isSuccessful) {
        Timber.v("unable to delete temporary cover ${file.absolutePath}")
      }
    }
    return file
  }

  companion object {
    const val COVER_DIR = "cover"
    const val TEMP_COVER = "temp_cover.jpg"
    const val JPEG_QUALITY = 100
  }
}

class ProtocolPingHandle(
  private val messageQueue: MessageQueue,
  private var activityChecker: SocketActivityChecker,
) : ProtocolAction {
  override suspend fun execute(message: ProtocolMessage) {
    activityChecker.ping()
    messageQueue.queue(SocketMessage.create(Protocol.Pong))
  }
}

class SimpleLogCommand : ProtocolAction {
  override suspend fun execute(message: ProtocolMessage) {
    Timber.d("handled message ${message.type}: ${message.data}")
  }
}

class UpdateNowPlayingTrackMoved(
  moshi: Moshi,
  dispatchers: AppCoroutineDispatchers,
  private val nowPlayingRepository: NowPlayingRepository,
) : ProtocolAction {
  private val scope = CoroutineScope(dispatchers.network)
  private val adapter = moshi.adapter(NowPlayingMoveResponse::class.java)

  override suspend fun execute(message: ProtocolMessage) {
    scope.launch {
      val response = adapter.fromJsonValue(message.data)
      if (response != null && response.success) {
        nowPlayingRepository.move(from = response.from + 1, to = response.to + 1)
      }
    }
  }
}

class UpdateNowPlayingTrackRemoval(
  moshi: Moshi,
  dispatchers: AppCoroutineDispatchers,
  private val nowPlayingRepository: NowPlayingRepository,
) : ProtocolAction {
  private val scope = CoroutineScope(dispatchers.network)
  private val adapter = moshi.adapter(NowPlayingTrackRemoveResponse::class.java)

  override suspend fun execute(message: ProtocolMessage) {
    scope.launch {
      val response = adapter.fromJsonValue(message.data)
      if (response != null && response.success) {
        nowPlayingRepository.remove(response.index + 1)
      }
    }
  }
}

class UpdatePlaybackPositionCommand(
  private val moshi: Moshi,
  private val appState: AppStatePublisher,
) : ProtocolAction {
  override suspend fun execute(message: ProtocolMessage) {
    val adapter = moshi.adapter(Position::class.java)
    val response = adapter.fromJsonValue(message.data) ?: return

    appState.updatePlayingPosition(
      PlayingPosition(
        response.current,
        response.total.coerceAtLeast(0),
      ),
    )
    val track = appState.playingTrack.first()
    if (track.duration != response.total) {
      appState.updatePlayingTrack(track.copy(duration = response.total))
    }
  }
}

class UpdateNowPlayingList(
  private val nowPlayingRepository: NowPlayingRepository,
) : ProtocolAction {
  override suspend fun execute(message: ProtocolMessage) {
    nowPlayingRepository.getRemote()
  }
}

class ProtocolVersionUpdate : ProtocolAction {
  override suspend fun execute(message: ProtocolMessage) {
    Timber.v(message.data.toString())
  }
}

fun ProtocolMessage.asBoolean(): Boolean = data as? Boolean == true
