package com.kelsos.mbrc.ui.navigation.library.artists

import com.kelsos.mbrc.library.artists.Artist
import com.kelsos.mbrc.mvp.BaseView
import com.kelsos.mbrc.mvp.Presenter
import com.raizlabs.android.dbflow.list.FlowCursorList

interface BrowseArtistView : BaseView {
  fun update(data: FlowCursorList<Artist>)
  fun failure(throwable: Throwable)
  fun hideLoading()
  fun showLoading()
}

interface BrowseArtistPresenter : Presenter<BrowseArtistView> {
  fun load()
  fun reload()
}

