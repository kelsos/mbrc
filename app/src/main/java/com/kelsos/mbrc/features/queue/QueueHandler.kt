package com.kelsos.mbrc.features.queue

import com.kelsos.mbrc.annotations.Queue
import com.kelsos.mbrc.common.utilities.AppCoroutineDispatchers
import com.kelsos.mbrc.features.library.Track
import com.kelsos.mbrc.features.library.TrackRepository
import com.kelsos.mbrc.features.player.CoverPayload
import com.kelsos.mbrc.features.settings.BasicSettingsHelper
import com.kelsos.mbrc.networking.ApiBase
import com.kelsos.mbrc.networking.protocol.Protocol
import kotlinx.coroutines.withContext
import timber.log.Timber

class QueueHandler(
  private val settings: BasicSettingsHelper,
  private val trackRepository: TrackRepository,
  private val service: ApiBase,
  private val dispatchers: AppCoroutineDispatchers,
) {
  private suspend fun queue(
    @Queue.Action type: String,
    tracks: List<String>,
    play: String? = null,
  ): Boolean {
    return withContext(dispatchers.io) {
      Timber.v("Queueing ${tracks.size} $type")
      try {
        val response =
          service.getItem(
            Protocol.NOW_PLAYING_QUEUE,
            QueueResponse::class,
            QueuePayload(type, tracks, play),
          )

        return@withContext response.code == CoverPayload.SUCCESS
      } catch (e: Exception) {
        Timber.e(e)
        return@withContext false
      }
    }
  }

  suspend fun queueAlbum(
    @Queue.Action type: String,
    album: String,
    artist: String,
  ): QueueResult {
    var tracks = 0
    var success = false
    try {
      val paths = trackRepository.getAlbumTrackPaths(album, artist)
      tracks = paths.size
      success = queue(type, paths)
    } catch (e: Exception) {
      Timber.e(e)
    }
    return QueueResult(success, tracks)
  }

  suspend fun queueArtist(
    @Queue.Action type: String,
    artist: String,
  ): QueueResult {
    var tracks = 0
    var success = false
    try {
      val paths = trackRepository.getArtistTrackPaths(artist)
      tracks = paths.size
      success = queue(type, paths)
    } catch (e: Exception) {
      Timber.e(e)
    }
    return QueueResult(success, tracks)
  }

  suspend fun queueGenre(
    @Queue.Action type: String,
    genre: String,
  ): QueueResult {
    var tracks = 0
    var success = false
    try {
      val paths = trackRepository.getGenreTrackPaths(genre)
      tracks = paths.size
      success = queue(type, paths)
    } catch (e: Exception) {
      Timber.e(e)
    }
    return QueueResult(success, tracks)
  }

  suspend fun queuePath(path: String): QueueResult {
    var success = false
    try {
      success = queue(Queue.NOW, listOf(path))
    } catch (e: Exception) {
      Timber.e(e)
    }
    return QueueResult(success, 1)
  }

  suspend fun queueTrack(
    track: Track,
    @Queue.Action type: String,
    queueAlbum: Boolean = false,
  ): QueueResult {
    val trackSource: List<String>
    val path: String?
    val success: Boolean
    var action = type
    trackSource =
      when (type) {
        Queue.ADD_ALL -> {
          path = track.src
          if (queueAlbum) {
            trackRepository.getAlbumTrackPaths(track.album!!, track.albumArtist!!)
          } else {
            trackRepository.getAllTrackPaths()
          }
        }
        Queue.PLAY_ALBUM -> {
          action = Queue.ADD_ALL
          path = track.src
          trackRepository.getAlbumTrackPaths(track.album!!, track.albumArtist!!)
        }
        Queue.PLAY_ARTIST -> {
          action = Queue.ADD_ALL
          path = track.src
          trackRepository.getArtistTrackPaths(track.artist!!)
        }
        else -> {
          path = null
          listOf(track.src!!)
        }
      }

    val tracks: Int = trackSource.size
    success = queue(action, trackSource, path)
    return QueueResult(success, tracks)
  }

  suspend fun queueTrack(
    track: Track,
    queueAlbum: Boolean = false,
  ): QueueResult = queueTrack(track, settings.defaultAction, queueAlbum)
}
