package com.kelsos.mbrc.ui.navigation.library.album_tracks

import com.kelsos.mbrc.annotations.Queue
import com.kelsos.mbrc.data.library.Track
import com.kelsos.mbrc.domain.AlbumInfo
import com.kelsos.mbrc.helper.QueueHandler
import com.kelsos.mbrc.mvp.BasePresenter
import com.kelsos.mbrc.repository.TrackRepository
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

class AlbumTracksPresenterImpl
  @Inject
  constructor(
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
