package com.kelsos.mbrc.features.library

import com.kelsos.mbrc.annotations.Queue
import com.kelsos.mbrc.common.mvp.BaseView
import com.kelsos.mbrc.common.mvp.Presenter
import com.raizlabs.android.dbflow.list.FlowCursorList

interface BrowseGenrePresenter : Presenter<BrowseGenreView> {
  fun load()

  fun sync()

  fun queue(
    @Queue.Action action: String,
    genre: Genre,
  )
}

interface BrowseGenreView : BaseView {
  fun update(cursor: FlowCursorList<Genre>)

  fun search(term: String)

  fun queue(
    success: Boolean,
    tracks: Int,
  )
}
