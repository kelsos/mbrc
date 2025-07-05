package com.kelsos.mbrc.features.library.genres

import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import androidx.paging.cachedIn
import com.kelsos.mbrc.common.state.ConnectionStateFlow
import com.kelsos.mbrc.features.library.BaseLibraryViewModel
import com.kelsos.mbrc.features.library.LibrarySearchModel
import com.kelsos.mbrc.features.library.LibrarySyncUseCase
import com.kelsos.mbrc.features.queue.Queue
import com.kelsos.mbrc.features.queue.QueueHandler
import com.kelsos.mbrc.features.settings.BasicSettingsHelper
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flatMapMerge
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

class BrowseGenreViewModel(
  private val repository: GenreRepository,
  private val librarySyncUseCase: LibrarySyncUseCase,
  private val queueHandler: QueueHandler,
  searchModel: LibrarySearchModel,
  settingsHelper: BasicSettingsHelper,
  connectionStateFlow: ConnectionStateFlow,
) : BaseLibraryViewModel<GenreUiMessage>(settingsHelper, connectionStateFlow) {
  val genres: Flow<PagingData<Genre>> =
    searchModel.term
      .flatMapMerge { term ->
        if (term.isEmpty()) {
          repository.getAll()
        } else {
          repository.search(term)
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

  fun queue(
    queue: Queue,
    genre: Genre,
  ) {
    if (queue === Queue.Default) {
      launchDefault(GenreUiMessage.OpenArtists(genre))
      return
    }

    viewModelScope.launch {
      if (!checkConnection()) {
        emit(GenreUiMessage.NetworkUnavailable)
        return@launch
      }

      val result = queueHandler.queueGenre(queue, genre.genre)
      val event =
        if (result.success) {
          GenreUiMessage.QueueSuccess(result.tracks)
        } else {
          GenreUiMessage.QueueFailed
        }
      emit(event)
    }
  }
}
