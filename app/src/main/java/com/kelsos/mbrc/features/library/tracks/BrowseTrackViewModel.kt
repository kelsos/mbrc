package com.kelsos.mbrc.features.library.tracks

import androidx.lifecycle.viewModelScope
import androidx.paging.cachedIn
import com.kelsos.mbrc.common.state.ConnectionStateFlow
import com.kelsos.mbrc.features.library.LibrarySearchModel
import com.kelsos.mbrc.features.library.LibrarySyncUseCase
import com.kelsos.mbrc.features.queue.QueueHandler
import com.kelsos.mbrc.features.settings.BasicSettingsHelper
import kotlinx.coroutines.flow.flatMapMerge
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

class BrowseTrackViewModel(
  private val repository: TrackRepository,
  private val librarySyncUseCase: LibrarySyncUseCase,
  queueHandler: QueueHandler,
  searchModel: LibrarySearchModel,
  settingsHelper: BasicSettingsHelper,
  connectionStateFlow: ConnectionStateFlow
) : BaseTrackViewModel(queueHandler, settingsHelper, connectionStateFlow) {
  override val tracks =
    searchModel.term
      .flatMapMerge {
        if (it.isEmpty()) {
          repository.getAll()
        } else {
          repository.search(it)
        }
      }.cachedIn(viewModelScope)

  val showSync = searchModel.term.map { it.isEmpty() }

  fun sync() {
    viewModelScope.launch {
      if (!checkConnection()) {
        emit(TrackUiMessage.NetworkUnavailable)
        return@launch
      }
      librarySyncUseCase.sync()
    }
  }
}
