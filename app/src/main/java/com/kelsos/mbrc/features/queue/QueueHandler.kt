package com.kelsos.mbrc.features.queue

import com.kelsos.mbrc.common.utilities.AppCoroutineDispatchers
import com.kelsos.mbrc.features.library.tracks.Track
import com.kelsos.mbrc.features.library.tracks.TrackQuery
import com.kelsos.mbrc.features.library.tracks.TrackRepository
import com.kelsos.mbrc.features.player.CoverPayload
import com.kelsos.mbrc.features.settings.SettingsManager
import com.kelsos.mbrc.networking.ApiBase
import com.kelsos.mbrc.networking.protocol.Protocol
import java.io.IOException
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import timber.log.Timber

class QueueHandler(
  private val settings: SettingsManager,
  private val trackRepository: TrackRepository,
  private val service: ApiBase,
  private val dispatchers: AppCoroutineDispatchers
) {
  private suspend fun queue(type: Queue, tracks: List<String>, play: String? = null): Boolean {
    return withContext(dispatchers.io) {
      Timber.v("Queueing ${tracks.size} $type")
      try {
        val response =
          service.getItem(
            Protocol.NowPlayingQueue,
            QueueResponse::class,
            QueuePayload(type.action, tracks, play)
          )

        return@withContext response.code == CoverPayload.SUCCESS
      } catch (e: IOException) {
        Timber.e(e)
        return@withContext false
      }
    }
  }

  suspend fun queueAlbum(type: Queue, album: String, artist: String): QueueResult {
    var tracks = 0
    var success = false
    try {
      val paths =
        withContext(dispatchers.database) {
          trackRepository.getTrackPaths(TrackQuery.Album(album, artist))
        }
      tracks = paths.size
      success = queue(type, paths)
    } catch (e: IOException) {
      Timber.e(e)
    }
    return QueueResult(success, tracks)
  }

  suspend fun queueArtist(type: Queue, artist: String): QueueResult {
    var tracks = 0
    var success = false
    try {
      val paths =
        withContext(dispatchers.database) {
          trackRepository.getTrackPaths(TrackQuery.Artist(artist))
        }
      tracks = paths.size
      success = queue(type, paths)
    } catch (e: IOException) {
      Timber.e(e)
    }
    return QueueResult(success, tracks)
  }

  suspend fun queueGenre(type: Queue, genre: String): QueueResult {
    var tracks = 0
    var success = false
    try {
      val paths =
        withContext(dispatchers.database) {
          trackRepository.getTrackPaths(TrackQuery.Genre(genre))
        }
      tracks = paths.size
      success = queue(type, paths)
    } catch (e: IOException) {
      Timber.e(e)
    }
    return QueueResult(success, tracks)
  }

  suspend fun queuePath(path: String): QueueResult {
    var success = false
    try {
      success = queue(Queue.Now, listOf(path))
    } catch (e: IOException) {
      Timber.e(e)
    }
    return QueueResult(success, 1)
  }

  suspend fun queueTrack(track: Track, type: Queue, queueAlbum: Boolean = false): QueueResult {
    val trackSource: List<String>
    val path: String?
    val success: Boolean
    var action = type
    trackSource =
      withContext(dispatchers.database) {
        when (type) {
          Queue.AddAll -> {
            path = track.src
            if (queueAlbum) {
              trackRepository.getTrackPaths(TrackQuery.Album(track.album, track.albumArtist))
            } else {
              trackRepository.getTrackPaths(TrackQuery.All)
            }
          }
          Queue.PlayAlbum -> {
            action = Queue.AddAll
            path = track.src
            trackRepository.getTrackPaths(TrackQuery.Album(track.album, track.albumArtist))
          }
          Queue.PlayArtist -> {
            action = Queue.AddAll
            path = track.src
            trackRepository.getTrackPaths(TrackQuery.Artist(track.artist))
          }
          else -> {
            path = null
            listOf(track.src)
          }
        }
      }

    val tracks: Int = trackSource.size
    success = queue(action, trackSource, path)
    return QueueResult(success, tracks)
  }

  suspend fun queueTrack(track: Track, queueAlbum: Boolean = false): QueueResult = queueTrack(
    track,
    Queue.fromTrackAction(settings.libraryTrackDefaultActionFlow.first()),
    queueAlbum
  )
}
