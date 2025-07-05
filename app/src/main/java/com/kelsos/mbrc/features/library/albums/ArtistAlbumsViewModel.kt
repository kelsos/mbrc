package com.kelsos.mbrc.features.library.albums

import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import androidx.paging.cachedIn
import com.kelsos.mbrc.common.state.ConnectionStateFlow
import com.kelsos.mbrc.features.queue.QueueHandler
import com.kelsos.mbrc.features.settings.BasicSettingsHelper
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.flatMapMerge
import kotlinx.coroutines.launch

class ArtistAlbumsViewModel(
  private val repository: AlbumRepository,
  queueHandler: QueueHandler,
  settingsHelper: BasicSettingsHelper,
  connectionStateFlow: ConnectionStateFlow,
) : BaseAlbumViewModel(queueHandler, settingsHelper, connectionStateFlow) {
  private val artistFilter: MutableSharedFlow<String> = MutableSharedFlow(replay = 1)

  override val albums: Flow<PagingData<Album>> =
    artistFilter
      .flatMapMerge { repository.getAlbumsByArtist(it) }
      .cachedIn(viewModelScope)

  fun load(artist: String) {
    viewModelScope.launch {
      artistFilter.emit(artist)
    }
  }
}
