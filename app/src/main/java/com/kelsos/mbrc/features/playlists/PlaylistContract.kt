package com.kelsos.mbrc.features.playlists

import com.kelsos.mbrc.common.mvp.BaseView
import com.kelsos.mbrc.common.mvp.Presenter
import com.raizlabs.android.dbflow.list.FlowCursorList

interface PlaylistView : BaseView {
  fun update(cursor: FlowCursorList<Playlist>)

  fun failure(throwable: Throwable)
}

interface PlaylistPresenter : Presenter<PlaylistView> {
  fun load()

  fun reload()

  fun play(path: String)
}
