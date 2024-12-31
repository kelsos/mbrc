package com.kelsos.mbrc.features.library

import com.kelsos.mbrc.annotations.Queue
import com.kelsos.mbrc.common.mvp.BaseView
import com.kelsos.mbrc.common.mvp.Presenter
import com.raizlabs.android.dbflow.list.FlowCursorList

interface BrowseAlbumView : BaseView {
  fun update(cursor: FlowCursorList<Album>)

  fun search(term: String)

  fun queue(
    success: Boolean,
    tracks: Int,
  )
}

interface BrowseAlbumPresenter : Presenter<BrowseAlbumView> {
  fun load()

  fun sync()

  fun queue(
    @Queue.Action action: String,
    entry: Album,
  )
}
