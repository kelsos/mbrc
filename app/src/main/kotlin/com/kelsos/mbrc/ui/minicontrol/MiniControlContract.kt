package com.kelsos.mbrc.ui.minicontrol

import com.kelsos.mbrc.content.activestatus.PlayerState.State
import com.kelsos.mbrc.content.library.tracks.PlayingTrackModel
import com.kelsos.mbrc.mvp.BaseView
import com.kelsos.mbrc.mvp.Presenter

interface MiniControlView : BaseView {
  fun updateTrackInfo(track: PlayingTrackModel)

  fun updateState(@State state: String)
}

interface MiniControlPresenter : Presenter<MiniControlView> {
  fun next()
  fun previous()
  fun playPause()
}