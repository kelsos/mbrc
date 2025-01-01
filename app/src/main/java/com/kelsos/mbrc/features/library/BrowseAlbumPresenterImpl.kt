package com.kelsos.mbrc.features.library

import com.kelsos.mbrc.common.mvp.BasePresenter
import com.kelsos.mbrc.events.bus.RxBus
import com.kelsos.mbrc.events.ui.LibraryRefreshCompleteEvent
import com.kelsos.mbrc.features.queue.QueueHandler
import kotlinx.coroutines.launch
import timber.log.Timber

class BrowseAlbumPresenterImpl(
  private val bus: RxBus,
  private val repository: AlbumRepository,
  private val librarySyncUseCase: LibrarySyncUseCase,
  private val queueHandler: QueueHandler,
  private val searchModel: LibrarySearchModel,
) : BasePresenter<BrowseAlbumView>(),
  BrowseAlbumPresenter {
  private fun updateUi(term: String) {
    scope.launch {
      view?.search(term)
      try {
        view?.update(getData(term))
      } catch (e: Exception) {
        Timber.v(e)
      }
    }
  }

  private suspend fun getData(term: String) = if (term.isNotEmpty()) repository.search(term) else repository.getAllCursor()

  override fun attach(view: BrowseAlbumView) {
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

  override fun sync() {
    scope.launch {
      librarySyncUseCase.sync()
    }
  }

  override fun queue(
    action: String,
    entry: Album,
  ) {
    scope.launch {
      val (success, tracks) = queueHandler.queueAlbum(action, entry.album!!, entry.artist!!)
      view?.queue(success, tracks)
    }
  }
}
