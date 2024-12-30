package com.kelsos.mbrc.ui.navigation.library

import com.kelsos.mbrc.mvp.BaseView
import com.kelsos.mbrc.mvp.Presenter

interface LibraryView : BaseView {
  fun syncFailure()
  fun showSyncProgress()
  fun hideSyncProgress()
  fun updateArtistOnlyPreference(albumArtistOnly: Boolean?)
  fun syncComplete(stats: LibraryStats)
  fun showStats(stats: LibraryStats)
}

interface LibraryPresenter : Presenter<LibraryView> {
  fun refresh()
  fun loadArtistPreference()
  fun setArtistPreference(albumArtistOnly: Boolean)
  fun search(keyword: String)
  fun showStats()
}
