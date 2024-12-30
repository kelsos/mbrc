package com.kelsos.mbrc.ui.mini_control

import com.kelsos.mbrc.annotations.PlayerState.State
import com.kelsos.mbrc.domain.TrackInfo
import com.kelsos.mbrc.mvp.BaseView
import com.kelsos.mbrc.mvp.Presenter

interface MiniControlView : BaseView {
  fun updateCover(path: String = "")

  fun updateTrackInfo(trackInfo: TrackInfo)

  fun updateState(@State state: String)
}

interface MiniControlPresenter : Presenter<MiniControlView> {
  fun load()
  fun next()
  fun previous()
  fun playPause()
}
