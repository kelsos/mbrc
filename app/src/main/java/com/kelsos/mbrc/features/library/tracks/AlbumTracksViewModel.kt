package com.kelsos.mbrc.features.library.tracks

import androidx.lifecycle.viewModelScope
import com.kelsos.mbrc.common.state.ConnectionStateFlow
import com.kelsos.mbrc.features.library.albums.AlbumInfo
import com.kelsos.mbrc.features.queue.Queue
import com.kelsos.mbrc.features.queue.QueueHandler
import com.kelsos.mbrc.features.settings.SettingsManager
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.flatMapMerge
import kotlinx.coroutines.launch

class AlbumTracksViewModel(
  private val repository: TrackRepository,
  private val queueHandler: QueueHandler,
  settingsManager: SettingsManager,
  connectionStateFlow: ConnectionStateFlow
) : BaseTrackViewModel(queueHandler, settingsManager, connectionStateFlow) {
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
      if (!checkConnection()) {
        emit(TrackUiMessage.NetworkUnavailable)
        return@launch
      }
      queueHandler.queueAlbum(
        type = Queue.Now,
        album = album.album,
        artist = album.artist
      )
    }
  }
}
