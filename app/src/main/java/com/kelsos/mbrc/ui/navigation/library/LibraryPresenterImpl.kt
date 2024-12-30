package com.kelsos.mbrc.ui.navigation.library

import com.kelsos.mbrc.events.bus.RxBus
import com.kelsos.mbrc.mvp.BasePresenter
import com.kelsos.mbrc.ui.navigation.library.LibrarySyncInteractor.OnCompleteListener
import com.kelsos.mbrc.utilities.SettingsManager
import kotlinx.coroutines.launch
import javax.inject.Inject

class LibraryPresenterImpl
  @Inject
  constructor(
    private val settingsManager: SettingsManager,
    private val bus: RxBus,
    private val librarySyncInteractor: LibrarySyncInteractor,
    private val searchModel: LibrarySearchModel,
  ) : BasePresenter<LibraryView>(),
    LibraryPresenter,
    OnCompleteListener,
    LibrarySyncInteractor.OnStartListener {
    override fun refresh() {
      view?.showSyncProgress()
      scope.launch {
        librarySyncInteractor.sync()
      }
    }

    override fun attach(view: LibraryView) {
      super.attach(view)
      librarySyncInteractor.setOnCompleteListener(this)
      librarySyncInteractor.setOnStartListener(this)
      if (!librarySyncInteractor.isRunning()) {
        view.hideSyncProgress()
      }
    }

    override fun detach() {
      super.detach()
      librarySyncInteractor.setOnCompleteListener(null)
      librarySyncInteractor.setOnStartListener(null)
    }

    override fun onTermination() {
      scope.launch {
        view?.hideSyncProgress()
      }
    }

    override fun onFailure(throwable: Throwable) {
      scope.launch {
        view?.syncFailure()
      }
    }

    override fun showStats() {
      scope.launch {
        view?.showStats(librarySyncInteractor.syncStats())
      }
    }

    override fun loadArtistPreference() {
      scope.launch {
        val shouldDisplay = settingsManager.shouldDisplayOnlyAlbumArtists()
        view?.updateArtistOnlyPreference(shouldDisplay)
      }
    }

    override fun setArtistPreference(albumArtistOnly: Boolean) {
      settingsManager.setShouldDisplayOnlyAlbumArtist(albumArtistOnly)
      bus.post(ArtistTabRefreshEvent())
    }

    override fun search(keyword: String) {
      searchModel.term.tryEmit(keyword)
    }

    override fun onSuccess(stats: LibraryStats) {
      view?.syncComplete(stats)
    }

    override fun onStart() {
      scope.launch {
        view?.showSyncProgress()
      }
    }
  }
