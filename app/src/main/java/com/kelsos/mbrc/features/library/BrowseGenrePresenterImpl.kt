package com.kelsos.mbrc.features.library

import com.kelsos.mbrc.common.mvp.BasePresenter
import com.kelsos.mbrc.events.bus.RxBus
import com.kelsos.mbrc.events.ui.LibraryRefreshCompleteEvent
import com.kelsos.mbrc.features.queue.QueueHandler
import com.raizlabs.android.dbflow.list.FlowCursorList
import kotlinx.coroutines.launch
import timber.log.Timber

class BrowseGenrePresenterImpl(
  private val bus: RxBus,
  private val repository: GenreRepository,
  private val librarySyncUseCase: LibrarySyncUseCase,
  private val queue: QueueHandler,
  private val searchModel: LibrarySearchModel,
) : BasePresenter<BrowseGenreView>(),
  BrowseGenrePresenter {
  override fun attach(view: BrowseGenreView) {
    super.attach(view)
    scope.launch {
      searchModel.term.collect { term -> updateUi(term) }
    }
    bus.register(this, LibraryRefreshCompleteEvent::class.java) { load() }
  }

  override fun detach() {
    super.detach()
    bus.unregister(this)
  }

  override fun load() {
    updateUi(searchModel.term.value)
  }

  private fun updateUi(term: String) {
    scope.launch {
      view?.search(term)
      try {
        view?.update(getData(term))
      } catch (e: Exception) {
        Timber.v(e, "Error while loading the data from the database")
      }
    }
  }

  private suspend fun getData(term: String): FlowCursorList<Genre> =
    if (term.isEmpty()) {
      repository.getAllCursor()
    } else {
      repository.search(term)
    }

  override fun sync() {
    scope.launch {
      librarySyncUseCase.sync()
    }
  }

  override fun queue(
    action: String,
    genre: Genre,
  ) {
    scope.launch {
      val genreName = genre.genre ?: throw IllegalArgumentException("null genre")
      val (success, tracks) = queue.queueGenre(action, genreName)
      view?.queue(success, tracks)
    }
  }
}
