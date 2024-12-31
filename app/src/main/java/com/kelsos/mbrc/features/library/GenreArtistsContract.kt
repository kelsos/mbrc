package com.kelsos.mbrc.features.library

import com.kelsos.mbrc.annotations.Queue
import com.kelsos.mbrc.common.mvp.BaseView
import com.kelsos.mbrc.common.mvp.Presenter
import com.raizlabs.android.dbflow.list.FlowCursorList

interface GenreArtistsView : BaseView {
  fun update(data: FlowCursorList<Artist>)

  fun queue(
    success: Boolean,
    tracks: Int,
  )
}

interface GenreArtistsPresenter : Presenter<GenreArtistsView> {
  fun load(genre: String)

  fun queue(
    @Queue.Action action: String,
    entry: Artist,
  )
}
