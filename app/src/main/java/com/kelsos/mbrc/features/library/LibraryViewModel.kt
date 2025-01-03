package com.kelsos.mbrc.features.library

import androidx.lifecycle.viewModelScope
import com.kelsos.mbrc.common.mvvm.BaseViewModel
import com.kelsos.mbrc.features.settings.SettingsManager
import kotlinx.coroutines.launch

class LibraryViewModel(
  private val searchModel: LibrarySearchModel,
  private val librarySyncUseCase: LibrarySyncUseCase,
  private val settingsManager: SettingsManager,
) : BaseViewModel<LibraryUiEvent>() {
  init {
    librarySyncUseCase.setOnCompleteListener(
      object : LibrarySyncUseCase.OnCompleteListener {
        override fun onTermination() {
          // figure out what to do here
        }

        override fun onFailure(throwable: Throwable) {
          viewModelScope.launch {
            emit(LibraryUiEvent.LibrarySyncFailed(throwable.message ?: "Unknown error"))
          }
        }

        override fun onSuccess(stats: LibraryStats) {
          viewModelScope.launch {
            emit(LibraryUiEvent.LibrarySyncComplete(stats))
          }
        }
      },
    )
  }

  fun search(string: String = "") {
    viewModelScope.launch {
      searchModel.term.emit(string)
    }
  }

  fun sync() {
    librarySyncUseCase.sync()
  }

  fun updateAlbumArtistOnly(bool: Boolean) {
    settingsManager.setShouldDisplayOnlyAlbumArtist(bool)
  }

  fun displayLibraryStats() {
    viewModelScope.launch {
      val stats = librarySyncUseCase.syncStats()
      emit(LibraryUiEvent.LibrarySyncComplete(stats))
    }
  }
}
