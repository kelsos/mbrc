package com.kelsos.mbrc.ui.navigation.library.gernes

import com.kelsos.mbrc.annotations.Queue
import com.kelsos.mbrc.data.library.Genre
import com.kelsos.mbrc.mvp.BaseView
import com.kelsos.mbrc.mvp.Presenter
import com.raizlabs.android.dbflow.list.FlowCursorList

interface BrowseGenrePresenter : Presenter<BrowseGenreView> {
  fun load()
  fun sync()
  fun queue(@Queue.Action action: String, genre: Genre)
}

interface BrowseGenreView : BaseView {
  fun update(cursor: FlowCursorList<Genre>)
  fun search(term: String)
  fun queue(success: Boolean, tracks: Int)
}
