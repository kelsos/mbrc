package com.kelsos.mbrc.features.library

import com.kelsos.mbrc.common.mvp.BasePresenter
import com.kelsos.mbrc.events.bus.RxBus
import com.kelsos.mbrc.features.settings.SettingsManager
import kotlinx.coroutines.launch

class LibraryPresenterImpl(
  private val settingsManager: SettingsManager,
  private val bus: RxBus,
  private val librarySyncUseCase: LibrarySyncUseCase,
  private val searchModel: LibrarySearchModel,
) : BasePresenter<LibraryView>(),
  LibraryPresenter,
  LibrarySyncUseCase.OnCompleteListener,
  LibrarySyncUseCase.OnStartListener {
  override fun refresh() {
    view?.showSyncProgress()
    scope.launch {
      librarySyncUseCase.sync()
    }
  }

  override fun attach(view: LibraryView) {
    super.attach(view)
    librarySyncUseCase.setOnCompleteListener(this)
    librarySyncUseCase.setOnStartListener(this)
    if (!librarySyncUseCase.isRunning()) {
      view.hideSyncProgress()
    }
  }

  override fun detach() {
    super.detach()
    librarySyncUseCase.setOnCompleteListener(null)
    librarySyncUseCase.setOnStartListener(null)
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
      view?.showStats(librarySyncUseCase.syncStats())
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
