package com.kelsos.mbrc.ui.navigation.main

import com.kelsos.mbrc.content.active_status.PlayerState.State
import com.kelsos.mbrc.content.active_status.Repeat.Mode
import com.kelsos.mbrc.content.library.tracks.TrackInfo
import com.kelsos.mbrc.events.ShuffleChange.ShuffleState
import com.kelsos.mbrc.events.UpdatePosition
import com.kelsos.mbrc.mvp.BaseView
import com.kelsos.mbrc.mvp.Presenter
import com.kelsos.mbrc.ui.navigation.main.LfmRating.Rating

interface MainView : BaseView {

  fun updateShuffleState(@ShuffleState shuffleState: String)

  fun updateRepeat(@Mode mode: String)

  fun updateVolume(volume: Int, mute: Boolean)

  fun updatePlayState(@State state: String)

  fun updateTrackInfo(info: TrackInfo)

  fun updateConnection(status: Int)

  fun updateScrobbleStatus(active: Boolean)

  fun updateLfmStatus(@Rating status: Int)

  fun updateCover(path: String)

  fun updateProgress(position: UpdatePosition)

  fun showChangeLog()

  fun notifyPluginOutOfDate()
}


interface MainViewPresenter : Presenter<MainView> {
  fun load()
  fun requestNowPlayingPosition()
  fun toggleScrobbling()
  fun seek(position: Int)
  fun play()
  fun previous()
  fun next()
  fun stop(): Boolean
  fun mute()
  fun shuffle()
  fun repeat()
  fun changeVolume(value: Int)
  fun lfmLove(): Boolean
}
