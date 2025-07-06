package com.kelsos.mbrc.features.library.albums

import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import com.kelsos.mbrc.common.state.ConnectionStateFlow
import com.kelsos.mbrc.features.library.BaseLibraryViewModel
import com.kelsos.mbrc.features.queue.Queue
import com.kelsos.mbrc.features.queue.QueueHandler
import com.kelsos.mbrc.features.settings.BasicSettingsHelper
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch

abstract class BaseAlbumViewModel(
  private val queueHandler: QueueHandler,
  settingsHelper: BasicSettingsHelper,
  connectionStateFlow: ConnectionStateFlow
) : BaseLibraryViewModel<AlbumUiMessage>(settingsHelper, connectionStateFlow) {
  abstract val albums: Flow<PagingData<Album>>

  fun queue(queue: Queue, album: Album) {
    if (queue == Queue.Default) {
      launchDefault(AlbumUiMessage.OpenAlbumTracks(album))
      return
    }

    viewModelScope.launch {
      if (!checkConnection()) {
        emit(AlbumUiMessage.NetworkUnavailable)
        return@launch
      }

      val result = queueHandler.queueAlbum(queue, album.album, album.artist)

      val message =
        if (result.success) {
          AlbumUiMessage.QueueSuccess(result.tracks)
        } else {
          AlbumUiMessage.QueueFailed
        }

      emit(message)
    }
  }
}
