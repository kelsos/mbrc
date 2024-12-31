package com.kelsos.mbrc.features.library

import com.kelsos.mbrc.annotations.Queue
import com.kelsos.mbrc.common.mvp.BaseView
import com.kelsos.mbrc.common.mvp.Presenter
import com.raizlabs.android.dbflow.list.FlowCursorList

interface ArtistAlbumsView : BaseView {
  fun update(albums: FlowCursorList<Album>)

  fun queue(
    success: Boolean,
    tracks: Int,
  )
}

interface ArtistAlbumsPresenter : Presenter<ArtistAlbumsView> {
  fun load(artist: String)

  fun queue(
    @Queue.Action action: String,
    album: Album,
  )
}
