package com.kelsos.mbrc.features.library

import com.kelsos.mbrc.annotations.Queue
import com.kelsos.mbrc.common.mvp.BaseView
import com.kelsos.mbrc.common.mvp.Presenter
import com.raizlabs.android.dbflow.list.FlowCursorList

interface BrowseTrackView : BaseView {
  fun update(it: FlowCursorList<Track>)

  fun search(term: String)

  fun queue(
    success: Boolean,
    tracks: Int,
  )
}

interface BrowseTrackPresenter : Presenter<BrowseTrackView> {
  fun load()

  fun sync()

  fun queue(
    track: Track,
    @Queue.Action action: String? = null,
  )
}
