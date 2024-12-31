package com.kelsos.mbrc.features.minicontrol

import com.kelsos.mbrc.annotations.PlayerState.State
import com.kelsos.mbrc.common.mvp.BaseView
import com.kelsos.mbrc.common.mvp.Presenter
import com.kelsos.mbrc.features.player.TrackInfo

interface MiniControlView : BaseView {
  fun updateCover(path: String = "")

  fun updateTrackInfo(trackInfo: TrackInfo)

  fun updateState(
    @State state: String,
  )
}

interface MiniControlPresenter : Presenter<MiniControlView> {
  fun load()

  fun next()

  fun previous()

  fun playPause()
}
