package com.kelsos.mbrc.ui.navigation.library.gernes

import com.kelsos.mbrc.library.genres.Genre
import com.kelsos.mbrc.mvp.BaseView
import com.kelsos.mbrc.mvp.Presenter
import com.raizlabs.android.dbflow.list.FlowCursorList

interface BrowseGenrePresenter : Presenter<BrowseGenreView> {
  fun load()
  fun reload()
}

interface BrowseGenreView : BaseView {
  fun update(cursor: FlowCursorList<Genre>)
  fun failure(it: Throwable)
  fun hideLoading()
  fun showLoading()
}
