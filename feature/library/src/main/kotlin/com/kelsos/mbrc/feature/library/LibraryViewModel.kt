package com.kelsos.mbrc.feature.library

import androidx.lifecycle.viewModelScope
import com.kelsos.mbrc.core.common.mvvm.BaseViewModel
import com.kelsos.mbrc.core.common.settings.LibrarySettings
import com.kelsos.mbrc.core.common.state.ConnectionStateFlow
import com.kelsos.mbrc.core.common.utilities.coroutines.AppCoroutineDispatchers
import com.kelsos.mbrc.core.networking.protocol.usecases.UserActionUseCase
import com.kelsos.mbrc.core.networking.protocol.usecases.playAllLibrary
import com.kelsos.mbrc.feature.library.domain.LibrarySyncProgress
import com.kelsos.mbrc.feature.library.domain.LibrarySyncUseCase
import com.kelsos.mbrc.feature.library.domain.LibrarySyncWorkHandler
import com.kelsos.mbrc.feature.library.domain.SyncOutcome
import com.kelsos.mbrc.feature.library.ui.LibraryUiEvent
import java.io.IOException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import timber.log.Timber

class LibraryViewModel(
  private val searchModel: LibrarySearchModel,
  private val librarySyncWorkHandler: LibrarySyncWorkHandler,
  private val librarySyncUseCase: LibrarySyncUseCase,
  private val librarySettings: LibrarySettings,
  private val connectionStateFlow: ConnectionStateFlow,
  private val userActionUseCase: UserActionUseCase,
  private val dispatchers: AppCoroutineDispatchers
) : BaseViewModel<LibraryUiEvent>() {
  // Cache flows to prevent creating new instances on every access
  val progress: Flow<LibrarySyncProgress> = librarySyncWorkHandler.syncProgress()

  val syncResults: Flow<SyncOutcome> = librarySyncWorkHandler.syncResults()

  val albumArtistsOnly: Flow<Boolean> = librarySettings.shouldDisplayOnlyArtists

  init {
    backfillDerivedTags()
  }

  /**
   * Backfills the derived tag junctions for installs upgraded from a schema that predates them, so
   * multi-value navigation works without waiting for a sync.
   *
   * Failure is logged and swallowed. This runs unprompted when the library screen opens, and the
   * screen is on the main navigation, so letting a database error escape would crash the app every
   * time the user goes near the library, with no way out. Browsing still works without the
   * junctions; only multi-value navigation degrades, and the next sync derives them again.
   */
  private fun backfillDerivedTags() {
    viewModelScope.launch(dispatchers.database) {
      runCatching { librarySyncUseCase.ensureDerived() }
        .onFailure { Timber.e(it, "Could not backfill the derived tag junctions") }
    }
  }

  fun search(string: String = "") {
    viewModelScope.launch {
      searchModel.setTerm(string)
    }
  }

  fun sync() {
    viewModelScope.launch {
      if (!connectionStateFlow.isConnected) {
        emit(LibraryUiEvent.NetworkUnavailable)
        return@launch
      }
      librarySyncWorkHandler.sync()
    }
  }

  fun updateAlbumArtistOnly(bool: Boolean) {
    viewModelScope.launch {
      librarySettings.setShouldDisplayOnlyAlbumArtist(bool)
    }
  }

  fun displayLibraryStats() {
    viewModelScope.launch {
      val syncStats = librarySyncUseCase.syncStats()
      emit(LibraryUiEvent.LibraryStatsReady(syncStats))
    }
  }

  fun playAll(shuffle: Boolean) {
    viewModelScope.launch(dispatchers.network) {
      if (!connectionStateFlow.isConnected) {
        emit(LibraryUiEvent.NetworkUnavailable)
        return@launch
      }
      try {
        userActionUseCase.playAllLibrary(shuffle)
        emit(LibraryUiEvent.PlayAllSuccess)
      } catch (e: IOException) {
        Timber.e(e)
        emit(LibraryUiEvent.PlayAllFailed)
      }
    }
  }
}
