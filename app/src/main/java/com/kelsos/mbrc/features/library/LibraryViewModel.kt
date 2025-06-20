package com.kelsos.mbrc.features.library

import androidx.lifecycle.viewModelScope
import com.kelsos.mbrc.common.mvvm.BaseViewModel
import com.kelsos.mbrc.features.settings.SettingsManager
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch

class LibraryViewModel(
  private val searchModel: LibrarySearchModel,
  private val librarySyncWorkHandler: LibrarySyncWorkHandler,
  private val librarySyncUseCase: LibrarySyncUseCase,
  private val settingsManager: SettingsManager,
) : BaseViewModel<LibraryUiEvent>() {
  val progress: Flow<LibrarySyncProgress>
    get() = librarySyncWorkHandler.syncProgress()

  val syncResults: Flow<SyncResult>
    get() = librarySyncWorkHandler.syncResults()

  fun search(string: String = "") {
    viewModelScope.launch {
      searchModel.term.emit(string)
    }
  }

  fun sync() {
    viewModelScope.launch {
      librarySyncWorkHandler.sync()
    }
  }

  fun updateAlbumArtistOnly(bool: Boolean) {
    settingsManager.setShouldDisplayOnlyAlbumArtist(bool)
  }

  fun displayLibraryStats() {
    viewModelScope.launch {
      val syncStats = librarySyncUseCase.syncStats()
      emit(LibraryUiEvent.LibraryStatsReady(syncStats))
    }
  }
}
