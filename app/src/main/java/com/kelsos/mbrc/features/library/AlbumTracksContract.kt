package com.kelsos.mbrc.features.library

import com.kelsos.mbrc.annotations.Queue
import com.kelsos.mbrc.common.mvp.BaseView
import com.kelsos.mbrc.common.mvp.Presenter
import com.raizlabs.android.dbflow.list.FlowCursorList

interface AlbumTracksView : BaseView {
  fun update(cursor: FlowCursorList<Track>)

  fun queue(
    success: Boolean,
    tracks: Int,
  )
}

interface AlbumTracksPresenter : Presenter<AlbumTracksView> {
  fun load(album: AlbumInfo)

  fun queue(
    entry: Track,
    @Queue.Action action: String? = null,
  )

  fun queueAlbum(
    artist: String,
    album: String,
  )
}
