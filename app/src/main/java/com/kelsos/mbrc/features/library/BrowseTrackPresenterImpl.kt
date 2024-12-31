package com.kelsos.mbrc.features.library

import com.kelsos.mbrc.common.mvp.BasePresenter
import com.kelsos.mbrc.events.bus.RxBus
import com.kelsos.mbrc.events.ui.LibraryRefreshCompleteEvent
import com.kelsos.mbrc.features.queue.QueueHandler
import com.raizlabs.android.dbflow.list.FlowCursorList
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

class BrowseTrackPresenterImpl
  @Inject
  constructor(
    private val bus: RxBus,
    private val repository: TrackRepository,
    private val librarySyncInteractor: LibrarySyncInteractor,
    private val queue: QueueHandler,
    private val searchModel: LibrarySearchModel,
  ) : BasePresenter<BrowseTrackView>(),
    BrowseTrackPresenter {
    override fun attach(view: BrowseTrackView) {
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

    private suspend fun getData(term: String): FlowCursorList<Track> =
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
      track: Track,
      action: String?,
    ) {
      scope.launch {
        val (success, tracks) =
          if (action == null) {
            queue.queueTrack(track)
          } else {
            queue.queueTrack(track, action)
          }
        view?.queue(success, tracks)
      }
    }
  }
