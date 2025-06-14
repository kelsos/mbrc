package com.kelsos.mbrc.features.library

import androidx.lifecycle.viewModelScope
import com.kelsos.mbrc.common.mvvm.BaseViewModel
import com.kelsos.mbrc.features.settings.SettingsManager
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import timber.log.Timber

class LibraryViewModel(
  private val searchModel: LibrarySearchModel,
  private val librarySyncWorkHandler: LibrarySyncWorkHandler,
  private val librarySyncUseCase: LibrarySyncUseCase,
  private val settingsManager: SettingsManager,
) : BaseViewModel<LibraryUiEvent>() {
  val progress: Flow<LibrarySyncProgress>
    get() = librarySyncWorkHandler.syncProgress()

  fun search(string: String = "") {
    viewModelScope.launch {
      searchModel.term.emit(string)
    }
  }

  fun sync() {
    viewModelScope.launch {
      librarySyncWorkHandler.sync().collect { syncResult ->
        Timber.v("SyncResult $syncResult")
        when (syncResult) {
          is SyncResult.Failed -> {
            emit(LibraryUiEvent.LibrarySyncFailed(syncResult.message))
          }
          SyncResult.Noop -> Unit
          is SyncResult.Success -> {
            emit(LibraryUiEvent.LibrarySyncComplete(syncResult.stats))
          }
        }
      }
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
