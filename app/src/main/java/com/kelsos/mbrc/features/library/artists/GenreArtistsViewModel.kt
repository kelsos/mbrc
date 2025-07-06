package com.kelsos.mbrc.features.library.artists

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

class GenreArtistsViewModel(
  private val repository: ArtistRepository,
  queueHandler: QueueHandler,
  settingsHelper: BasicSettingsHelper,
  connectionStateFlow: ConnectionStateFlow
) : BaseArtistViewModel(queueHandler, settingsHelper, connectionStateFlow) {
  private val genre: MutableSharedFlow<Long> = MutableSharedFlow(replay = 1)

  override val artists: Flow<PagingData<Artist>> =
    genre
      .flatMapMerge { id ->
        repository.getArtistByGenre(id)
      }.cachedIn(viewModelScope)

  fun load(id: Long) {
    viewModelScope.launch {
      genre.emit(id)
    }
  }
}
