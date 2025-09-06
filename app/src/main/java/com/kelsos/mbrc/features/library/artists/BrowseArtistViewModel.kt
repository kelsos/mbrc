package com.kelsos.mbrc.features.library.artists

import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import androidx.paging.cachedIn
import com.kelsos.mbrc.common.state.ConnectionStateFlow
import com.kelsos.mbrc.features.library.LibrarySearchModel
import com.kelsos.mbrc.features.library.LibrarySyncUseCase
import com.kelsos.mbrc.features.queue.QueueHandler
import com.kelsos.mbrc.features.settings.SettingsManager
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapMerge
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

private data class ArtistSearchParams(val keyword: String, val albumArtists: Boolean)

class BrowseArtistViewModel(
  private val repository: ArtistRepository,
  private val librarySyncUseCase: LibrarySyncUseCase,
  searchModel: LibrarySearchModel,
  settingsManager: SettingsManager,
  queueHandler: QueueHandler,
  connectionStateFlow: ConnectionStateFlow
) : BaseArtistViewModel(queueHandler, settingsManager, connectionStateFlow) {
  val shouldDisplayOnlyArtists = settingsManager.shouldDisplayOnlyArtists

  override val artists: Flow<PagingData<Artist>> =
    searchModel.term
      .combine(shouldDisplayOnlyArtists) { keyword, albumArtists ->
        ArtistSearchParams(keyword, albumArtists)
      }.flatMapMerge { (keyword, albumArtists) ->
        if (keyword.isEmpty()) {
          if (albumArtists) {
            repository.getAlbumArtistsOnly()
          } else {
            repository.getAll()
          }
        } else {
          repository.search(keyword)
        }
      }.cachedIn(viewModelScope)

  val showSync = searchModel.term.map { it.isEmpty() }

  fun sync() {
    viewModelScope.launch {
      if (!checkConnection()) {
        emit(ArtistUiMessage.NetworkUnavailable)
        return@launch
      }
      librarySyncUseCase.sync()
    }
  }
}
