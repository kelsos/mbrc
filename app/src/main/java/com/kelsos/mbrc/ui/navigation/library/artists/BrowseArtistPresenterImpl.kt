package com.kelsos.mbrc.ui.navigation.library.artists

import com.kelsos.mbrc.data.library.Artist
import com.kelsos.mbrc.events.bus.RxBus
import com.kelsos.mbrc.events.ui.LibraryRefreshCompleteEvent
import com.kelsos.mbrc.helper.QueueHandler
import com.kelsos.mbrc.mvp.BasePresenter
import com.kelsos.mbrc.repository.ArtistRepository
import com.kelsos.mbrc.ui.navigation.library.ArtistTabRefreshEvent
import com.kelsos.mbrc.ui.navigation.library.LibrarySearchModel
import com.kelsos.mbrc.ui.navigation.library.LibrarySyncInteractor
import com.kelsos.mbrc.utilities.SettingsManager
import com.raizlabs.android.dbflow.list.FlowCursorList
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

class BrowseArtistPresenterImpl
@Inject
constructor(
  private val bus: RxBus,
  private val repository: ArtistRepository,
  private val settingsManager: SettingsManager,
  private val librarySyncInteractor: LibrarySyncInteractor,
  private val queue: QueueHandler,
  private val searchModel: LibrarySearchModel
) : BasePresenter<BrowseArtistView>(), BrowseArtistPresenter {

  override fun attach(view: BrowseArtistView) {
    super.attach(view)
    scope.launch {
      searchModel.term.collect { term -> updateUi(term) }
    }
    bus.register(this, LibraryRefreshCompleteEvent::class.java) { load() }
    bus.register(this, ArtistTabRefreshEvent::class.java) { load() }
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

  private suspend fun getData(term: String): FlowCursorList<Artist> {
    return if (term.isEmpty()) {
      val shouldDisplay = settingsManager.shouldDisplayOnlyAlbumArtists()
      if (shouldDisplay) {
        repository.getAlbumArtistsOnly()
      } else {
        repository.getAllCursor()
      }
    } else {
      repository.search(term)
    }
  }

  override fun sync() {
    scope.launch {
      librarySyncInteractor.sync()
    }
  }

  override fun queue(action: String, entry: Artist) {
    scope.launch {
      val artist = entry.artist ?: throw IllegalArgumentException("artist should not be null")
      val (success, tracks) = queue.queueArtist(action, artist)
      view?.queue(success, tracks)
    }
  }

}
