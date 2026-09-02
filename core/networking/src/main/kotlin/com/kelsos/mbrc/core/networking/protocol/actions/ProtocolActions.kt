package com.kelsos.mbrc.core.networking.protocol.actions

import com.kelsos.mbrc.core.common.state.BasicTrackInfo
import com.kelsos.mbrc.core.common.state.LfmRating
import com.kelsos.mbrc.core.common.state.PlayerState
import com.kelsos.mbrc.core.common.state.PlayerStatusModel
import com.kelsos.mbrc.core.common.state.PlayingPosition
import com.kelsos.mbrc.core.common.state.Repeat
import com.kelsos.mbrc.core.common.state.ShuffleMode
import com.kelsos.mbrc.core.common.state.TrackRating
import com.kelsos.mbrc.core.common.state.toBasicTrackInfo
import com.kelsos.mbrc.core.networking.SocketActivityChecker
import com.kelsos.mbrc.core.networking.client.MessageQueue
import com.kelsos.mbrc.core.networking.client.SocketMessage
import com.kelsos.mbrc.core.networking.protocol.base.Protocol
import com.kelsos.mbrc.core.networking.protocol.base.ProtocolAction
import com.kelsos.mbrc.core.networking.protocol.base.ProtocolMessage
import com.kelsos.mbrc.core.networking.protocol.base.asBoolean
import com.kelsos.mbrc.core.networking.protocol.models.NowPlayingTrack
import com.kelsos.mbrc.core.networking.protocol.models.PlayerStatus
import com.kelsos.mbrc.core.networking.protocol.models.Position
import com.kelsos.mbrc.core.networking.protocol.payloads.CoverPayload
import com.kelsos.mbrc.core.networking.protocol.payloads.LyricsPayload
import com.kelsos.mbrc.core.networking.protocol.payloads.NowPlayingDetailsPayload
import com.kelsos.mbrc.core.networking.protocol.payloads.NowPlayingMoveResponse
import com.kelsos.mbrc.core.networking.protocol.payloads.NowPlayingTrackRemoveResponse
import com.squareup.moshi.Moshi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.firstOrNull
import timber.log.Timber

/**
 * The range `PlayerStatusModel.volume` declares with `@IntRange`, which nothing else enforces.
 * Inbound values are clamped to it so a plugin reporting something outside cannot drive the volume
 * slider off its track.
 */
private val VOLUME_RANGE = 0..100

class UpdateLastFm(private val stateHandler: PlayerStateHandler) : ProtocolAction {
  override suspend fun execute(message: ProtocolMessage) {
    val previousState = stateHandler.playerStatus.firstOrNull() ?: PlayerStatusModel()
    stateHandler.updatePlayerStatus(previousState.copy(scrobbling = message.asBoolean()))
  }
}

class UpdateLfmRating(private val stateHandler: PlayerStateHandler) : ProtocolAction {
  override suspend fun execute(message: ProtocolMessage) {
    val previousState = stateHandler.playingTrackRating.firstOrNull() ?: TrackRating()
    val lfmRating = LfmRating.fromString(message.data as? String)
    stateHandler.updateTrackRating(previousState.copy(lfmRating = lfmRating))
  }
}

class UpdateLyrics(mapper: Moshi, private val stateHandler: PlayerStateHandler) : ProtocolAction {
  private val adapter = mapper.adapter(LyricsPayload::class.java)

  override suspend fun execute(message: ProtocolMessage) {
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

    stateHandler.updateLyrics(lyrics)
  }

  companion object {
    private const val LYRICS_NEWLINE = "\r\n|\n"
  }
}

class UpdateMute(private val stateHandler: PlayerStateHandler) : ProtocolAction {
  override suspend fun execute(message: ProtocolMessage) {
    val previousState = stateHandler.playerStatus.firstOrNull() ?: PlayerStatusModel()
    stateHandler.updatePlayerStatus(previousState.copy(mute = message.asBoolean()))
  }
}

class UpdateNowPlayingTrack(
  private val stateHandler: PlayerStateHandler,
  private val notifier: TrackChangeNotifier,
  mapper: Moshi
) : ProtocolAction {
  private val adapter = mapper.adapter(NowPlayingTrack::class.java)

  override suspend fun execute(message: ProtocolMessage) {
    val track = adapter.fromJsonValue(message.data) ?: return
    val previousState =
      stateHandler.playingTrack.firstOrNull()?.toBasicTrackInfo() ?: BasicTrackInfo()
    val newState =
      previousState.copy(
        artist = track.artist,
        title = track.title,
        album = track.album,
        year = track.year,
        path = track.path
      )
    stateHandler.updatePlayingTrack(newState)
    notifier.notifyTrackChanged(newState)
    notifier.persistTrackInfo(newState)
    notifier.requestTrackDetails()
  }
}

class UpdatePlayerStatus(private val stateHandler: PlayerStateHandler, moshi: Moshi) :
  ProtocolAction {
  private val adapter = moshi.adapter(PlayerStatus::class.java)

  override suspend fun execute(message: ProtocolMessage) {
    val status = adapter.fromJsonValue(message.data) ?: return
    val previousState = stateHandler.playerStatus.firstOrNull() ?: PlayerStatusModel()
    stateHandler.updatePlayerStatus(
      previousState.copy(
        mute = status.mute,
        state = PlayerState.fromString(status.playState),
        repeat = Repeat.fromString(status.repeat),
        shuffle = ShuffleMode.fromString(status.shuffle),
        scrobbling = status.scrobbling,
        volume = status.volume.coerceIn(VOLUME_RANGE)
      )
    )
  }
}

class UpdatePlayState(
  private val stateHandler: PlayerStateHandler,
  private val notifier: TrackChangeNotifier
) : ProtocolAction {
  override suspend fun execute(message: ProtocolMessage) {
    val playState = PlayerState.fromString(message.data as? String)
    val previousState = stateHandler.playerStatus.firstOrNull() ?: PlayerStatusModel()
    stateHandler.updatePlayerStatus(previousState.copy(state = playState))
    notifier.notifyPlayStateChanged(playState)
  }
}

class UpdatePluginVersionCommand(private val pluginVersionHandler: PluginVersionHandler) :
  ProtocolAction {
  override suspend fun execute(message: ProtocolMessage) {
    val pluginVersion = message.data as? String
    Timber.v("plugin reports $pluginVersion")
    pluginVersion?.let { version ->
      pluginVersionHandler.onVersionReceived(version)
    }
  }
}

class UpdateRating(private val stateHandler: PlayerStateHandler) : ProtocolAction {
  override suspend fun execute(message: ProtocolMessage) {
    val dataString = message.data.toString()
    // Empty string means unrated (null), otherwise parse as float
    // 0 = bomb rating, 0.5-5.0 = star rating
    val rating: Float? = when {
      dataString.isEmpty() || dataString == "null" -> null
      else -> dataString.toFloatOrNull()
    }
    val previousState = stateHandler.playingTrackRating.firstOrNull() ?: TrackRating()
    stateHandler.updateTrackRating(previousState.copy(rating = rating))
  }
}

class UpdateRepeat(private val stateHandler: PlayerStateHandler) : ProtocolAction {
  override suspend fun execute(message: ProtocolMessage) {
    val repeat = Repeat.fromString(message.data as? String)
    val previousState = stateHandler.playerStatus.firstOrNull() ?: PlayerStatusModel()
    stateHandler.updatePlayerStatus(previousState.copy(repeat = repeat))
  }
}

class UpdateShuffle(private val stateHandler: PlayerStateHandler) : ProtocolAction {
  override suspend fun execute(message: ProtocolMessage) {
    val data = ShuffleMode.fromString(message.data as? String)
    val previousState = stateHandler.playerStatus.firstOrNull() ?: PlayerStatusModel()
    stateHandler.updatePlayerStatus(previousState.copy(shuffle = data))
  }
}

/**
 * Applies a volume the plugin reports, sent as either a number or a string.
 *
 * A value that is neither leaves the volume untouched. It used to be cast straight to `Number`, so
 * anything else threw and the update was dropped by the dispatcher's catch-all.
 */
class UpdateVolume(private val stateHandler: PlayerStateHandler) : ProtocolAction {
  override suspend fun execute(message: ProtocolMessage) {
    val volume = message.data.toString().toDoubleOrNull()?.toInt() ?: return
    val previousState = stateHandler.playerStatus.firstOrNull() ?: PlayerStatusModel()
    stateHandler.updatePlayerStatus(previousState.copy(volume = volume.coerceIn(VOLUME_RANGE)))
  }
}

class UpdateCover(
  moshi: Moshi,
  private val coverHandler: CoverHandler,
  private val stateHandler: PlayerStateHandler
) : ProtocolAction {
  private val adapter = moshi.adapter(CoverPayload::class.java)

  override suspend fun execute(message: ProtocolMessage) {
    val payload = adapter.fromJsonValue(message.data) ?: return
    val previousState =
      stateHandler.playingTrack.firstOrNull()?.toBasicTrackInfo() ?: BasicTrackInfo()

    val coverUri = when (payload.status) {
      CoverPayload.NOT_FOUND -> {
        coverHandler.clearCovers()
        ""
      }

      CoverPayload.READY -> {
        coverHandler.fetchAndStoreCover()
      }

      else -> return
    }

    stateHandler.updatePlayingTrack(previousState.copy(coverUrl = coverUri))
    Timber.v("Message received for cover, status: ${payload.status}")
  }
}

class ProtocolPingHandle(
  private val messageQueue: MessageQueue,
  private val activityChecker: SocketActivityChecker
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

class UpdateNowPlayingTrackMoved(moshi: Moshi) : ProtocolAction {
  private val adapter = moshi.adapter(NowPlayingMoveResponse::class.java)

  override suspend fun execute(message: ProtocolMessage) {
    val response = adapter.fromJsonValue(message.data)
    if (response != null && response.success) {
      Timber.v("confirmed successful move from ${response.from + 1} to ${response.to + 1}")
    }
  }
}

class UpdateNowPlayingTrackRemoval(moshi: Moshi, private val nowPlayingHandler: NowPlayingHandler) :
  ProtocolAction {
  private val adapter = moshi.adapter(NowPlayingTrackRemoveResponse::class.java)

  override suspend fun execute(message: ProtocolMessage) {
    val response = adapter.fromJsonValue(message.data)
    if (response != null && response.success) {
      nowPlayingHandler.removeTrack(response.index + 1)
    }
  }
}

class UpdatePlaybackPositionCommand(moshi: Moshi, private val stateHandler: PlayerStateHandler) :
  ProtocolAction {
  private val adapter = moshi.adapter(Position::class.java)

  override suspend fun execute(message: ProtocolMessage) {
    val response = adapter.fromJsonValue(message.data) ?: return
    Timber.d("Position: current=%d, total=%d", response.current, response.total)

    stateHandler.updatePlayingPosition(
      PlayingPosition(
        response.current,
        response.total
      )
    )
    val track = stateHandler.playingTrack.first()
    if (track.duration != response.total) {
      stateHandler.updatePlayingTrack(track.toBasicTrackInfo().copy(duration = response.total))
    }
  }
}

class UpdateNowPlayingList(private val nowPlayingHandler: NowPlayingHandler) : ProtocolAction {
  override suspend fun execute(message: ProtocolMessage) {
    nowPlayingHandler.refreshFromRemote()
  }
}

class ProtocolVersionUpdate : ProtocolAction {
  override suspend fun execute(message: ProtocolMessage) {
    Timber.v(message.data.toString())
  }
}

class UpdateNowPlayingDetails(moshi: Moshi, private val stateHandler: PlayerStateHandler) :
  ProtocolAction {
  private val adapter = moshi.adapter(NowPlayingDetailsPayload::class.java)

  override suspend fun execute(message: ProtocolMessage) {
    val payload = adapter.fromJsonValue(message.data) ?: return
    stateHandler.updateTrackDetails(payload.toTrackDetails())
  }
}
