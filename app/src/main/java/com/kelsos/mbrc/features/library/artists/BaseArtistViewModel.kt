package com.kelsos.mbrc.features.library.artists

import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import com.kelsos.mbrc.common.state.ConnectionStateFlow
import com.kelsos.mbrc.features.library.BaseLibraryViewModel
import com.kelsos.mbrc.features.queue.Queue
import com.kelsos.mbrc.features.queue.QueueHandler
import com.kelsos.mbrc.features.settings.SettingsManager
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch

abstract class BaseArtistViewModel(
  private val queueHandler: QueueHandler,
  settingsManager: SettingsManager,
  connectionStateFlow: ConnectionStateFlow
) : BaseLibraryViewModel<ArtistUiMessage>(settingsManager, connectionStateFlow) {
  abstract val artists: Flow<PagingData<Artist>>

  fun queue(queue: Queue, artist: Artist) {
    if (queue == Queue.Default) {
      launchDefault(ArtistUiMessage.OpenArtistAlbums(artist))
      return
    }

    viewModelScope.launch {
      if (!checkConnection()) {
        emit(ArtistUiMessage.NetworkUnavailable)
        return@launch
      }

      val result = queueHandler.queueArtist(queue, artist.artist)

      val message =
        if (result.success) {
          ArtistUiMessage.QueueSuccess(result.tracks)
        } else {
          ArtistUiMessage.QueueFailed
        }

      emit(message)
    }
  }
}
