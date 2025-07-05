package com.kelsos.mbrc.features.library.albums

import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import androidx.paging.cachedIn
import com.kelsos.mbrc.common.state.ConnectionStateFlow
import com.kelsos.mbrc.features.library.LibrarySearchModel
import com.kelsos.mbrc.features.library.LibrarySyncUseCase
import com.kelsos.mbrc.features.queue.QueueHandler
import com.kelsos.mbrc.features.settings.BasicSettingsHelper
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flatMapMerge
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

class BrowseAlbumViewModel(
  private val repository: AlbumRepository,
  private val librarySyncUseCase: LibrarySyncUseCase,
  queueHandler: QueueHandler,
  settingsHelper: BasicSettingsHelper,
  connectionStateFlow: ConnectionStateFlow,
  searchModel: LibrarySearchModel,
) : BaseAlbumViewModel(queueHandler, settingsHelper, connectionStateFlow) {
  override val albums: Flow<PagingData<Album>> =
    searchModel.term
      .flatMapMerge { keyword ->
        if (keyword.isEmpty()) {
          repository.getAll()
        } else {
          repository.search(keyword)
        }
      }.cachedIn(viewModelScope)

  val showSync = searchModel.term.map { it.isEmpty() }

  fun sync() {
    viewModelScope.launch {
      if (!checkConnection()) {
        emit(AlbumUiMessage.NetworkUnavailable)
        return@launch
      }
      librarySyncUseCase.sync()
    }
  }
}
