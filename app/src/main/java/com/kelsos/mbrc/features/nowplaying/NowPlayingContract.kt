package com.kelsos.mbrc.features.nowplaying

import com.kelsos.mbrc.common.mvp.BaseView
import com.kelsos.mbrc.common.mvp.Presenter
import com.kelsos.mbrc.features.player.TrackInfo
import com.raizlabs.android.dbflow.list.FlowCursorList

interface NowPlayingView : BaseView {
  fun update(cursor: FlowCursorList<NowPlaying>)

  fun reload()

  fun loading()

  fun trackChanged(
    trackInfo: TrackInfo,
    scrollToTrack: Boolean = false,
  )

  fun failure(throwable: Throwable)
}

interface NowPlayingPresenter : Presenter<NowPlayingView> {
  fun reload(scrollToTrack: Boolean)

  fun play(position: Int)

  fun moveTrack(
    from: Int,
    to: Int,
  )

  fun removeTrack(position: Int)

  fun load()

  fun search(query: String)
}
