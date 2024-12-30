package com.kelsos.mbrc.ui.navigation.library.tracks

import com.kelsos.mbrc.annotations.Queue
import com.kelsos.mbrc.data.library.Track
import com.kelsos.mbrc.mvp.BaseView
import com.kelsos.mbrc.mvp.Presenter
import com.raizlabs.android.dbflow.list.FlowCursorList

interface BrowseTrackView : BaseView {
  fun update(it: FlowCursorList<Track>)
  fun search(term: String)
  fun queue(success: Boolean, tracks: Int)
}

interface BrowseTrackPresenter : Presenter<BrowseTrackView> {
  fun load()
  fun sync()
  fun queue(track: Track, @Queue.Action action: String? = null)
}
