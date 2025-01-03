package com.kelsos.mbrc.features.library.tracks

import androidx.lifecycle.viewModelScope
import com.kelsos.mbrc.features.library.albums.AlbumInfo
import com.kelsos.mbrc.features.queue.Queue
import com.kelsos.mbrc.features.queue.QueueHandler
import com.kelsos.mbrc.features.settings.BasicSettingsHelper
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.flatMapMerge
import kotlinx.coroutines.launch

class AlbumTracksViewModel(
  private val repository: TrackRepository,
  private val queueHandler: QueueHandler,
  settingsHelper: BasicSettingsHelper,
) : BaseTrackViewModel(queueHandler, settingsHelper) {
  private val albumInfo = MutableSharedFlow<AlbumInfo>(replay = 1)

  override val tracks =
    albumInfo.flatMapMerge {
      val query =
        if (it.album.isEmpty()) {
          PagingTrackQuery.NonAlbum(artist = it.artist)
        } else {
          PagingTrackQuery.Album(artist = it.artist, album = it.album)
        }
      repository.getTracks(query)
    }

  fun load(album: AlbumInfo) {
    viewModelScope.launch {
      albumInfo.emit(album)
    }
  }

  fun queueAlbum(album: AlbumInfo) {
    viewModelScope.launch {
      queueHandler.queueAlbum(
        type = Queue.Now,
        album = album.album,
        artist = album.artist,
      )
    }
  }
}
