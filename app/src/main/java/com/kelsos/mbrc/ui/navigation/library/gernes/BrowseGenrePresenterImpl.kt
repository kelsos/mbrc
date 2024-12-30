package com.kelsos.mbrc.ui.navigation.library.gernes

import com.kelsos.mbrc.data.library.Genre
import com.kelsos.mbrc.events.bus.RxBus
import com.kelsos.mbrc.events.ui.LibraryRefreshCompleteEvent
import com.kelsos.mbrc.helper.QueueHandler
import com.kelsos.mbrc.mvp.BasePresenter
import com.kelsos.mbrc.repository.GenreRepository
import com.kelsos.mbrc.ui.navigation.library.LibrarySearchModel
import com.kelsos.mbrc.ui.navigation.library.LibrarySyncInteractor
import com.raizlabs.android.dbflow.list.FlowCursorList
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

class BrowseGenrePresenterImpl
  @Inject
  constructor(
    private val bus: RxBus,
    private val repository: GenreRepository,
    private val librarySyncInteractor: LibrarySyncInteractor,
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
        librarySyncInteractor.sync()
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
