package com.kelsos.mbrc.features.library

import com.kelsos.mbrc.common.mvp.BaseView
import com.kelsos.mbrc.common.mvp.Presenter

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
