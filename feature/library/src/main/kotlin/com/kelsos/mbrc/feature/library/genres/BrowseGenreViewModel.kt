package com.kelsos.mbrc.feature.library.genres

import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import androidx.paging.cachedIn
import com.kelsos.mbrc.core.common.settings.GenreSortPreference
import com.kelsos.mbrc.core.common.settings.LibrarySettings
import com.kelsos.mbrc.core.common.state.ConnectionStateFlow
import com.kelsos.mbrc.core.data.library.genre.Genre
import com.kelsos.mbrc.core.data.library.genre.GenreRepository
import com.kelsos.mbrc.feature.library.LibrarySearchModel
import com.kelsos.mbrc.feature.library.domain.LibrarySyncUseCase
import com.kelsos.mbrc.feature.library.queue.QueueHandler
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

@OptIn(ExperimentalCoroutinesApi::class)
class BrowseGenreViewModel(
  private val repository: GenreRepository,
  private val librarySyncUseCase: LibrarySyncUseCase,
  queueHandler: QueueHandler,
  private val searchModel: LibrarySearchModel,
  private val librarySettings: LibrarySettings,
  connectionStateFlow: ConnectionStateFlow
) : BaseGenreViewModel(queueHandler, librarySettings, connectionStateFlow) {

  val sortPreference: Flow<GenreSortPreference> = librarySettings.genreSortPreferenceFlow

  override val genres: Flow<PagingData<Genre>> =
    combine(searchModel.term, sortPreference) { term, sort -> term to sort }
      .flatMapLatest { (term, sort) ->
        if (term.isEmpty()) {
          repository.getAll(sort.order)
        } else {
          repository.search(term, sort.order)
        }
      }.cachedIn(viewModelScope)

  val showSync = searchModel.term.map { it.isEmpty() }

  fun sync() {
    viewModelScope.launch {
      if (!checkConnection()) {
        emit(GenreUiMessage.NetworkUnavailable)
        return@launch
      }
      librarySyncUseCase.sync()
    }
  }

  fun updateSortPreference(preference: GenreSortPreference) {
    viewModelScope.launch {
      librarySettings.setGenreSortPreference(preference)
    }
  }
}
