package com.kelsos.mbrc.features.library

import com.kelsos.mbrc.common.mvp.BaseView
import com.kelsos.mbrc.common.mvp.Presenter
import com.raizlabs.android.dbflow.list.FlowCursorList

interface BrowseArtistView : BaseView {
  fun update(data: FlowCursorList<Artist>)

  fun search(term: String)

  fun queue(
    success: Boolean,
    tracks: Int,
  )
}

interface BrowseArtistPresenter : Presenter<BrowseArtistView> {
  fun load()

  fun sync()

  fun queue(
    action: String,
    entry: Artist,
  )
}
