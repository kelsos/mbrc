package com.kelsos.mbrc.features.library

import com.kelsos.mbrc.annotations.Queue
import com.kelsos.mbrc.common.mvp.BasePresenter
import com.kelsos.mbrc.features.queue.QueueHandler
import kotlinx.coroutines.launch
import timber.log.Timber

class AlbumTracksPresenterImpl(
  private val repository: TrackRepository,
  private val queue: QueueHandler,
) : BasePresenter<AlbumTracksView>(),
  AlbumTracksPresenter {
  override fun load(album: AlbumInfo) {
    scope.launch {
      try {
        view?.update(
          when {
            album.album.isEmpty() -> {
              repository.getNonAlbumTracks(album.artist)
            }
            else -> {
              repository.getAlbumTracks(album.album, album.artist)
            }
          },
        )
      } catch (e: Exception) {
        Timber.v(e)
      }
    }
  }

  override fun queue(
    entry: Track,
    action: String?,
  ) {
    scope.launch {
      val (success, tracks) =
        if (action == null) {
          queue.queueTrack(entry, true)
        } else {
          queue.queueTrack(entry, action, true)
        }
      view?.queue(success, tracks)
    }
  }

  override fun queueAlbum(
    artist: String,
    album: String,
  ) {
    scope.launch {
      val (success, tracks) = queue.queueAlbum(Queue.NOW, album, artist)
      view?.queue(success, tracks)
    }
  }
}
