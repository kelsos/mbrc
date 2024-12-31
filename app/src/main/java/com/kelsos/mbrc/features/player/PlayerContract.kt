package com.kelsos.mbrc.features.player

import com.kelsos.mbrc.annotations.PlayerState.State
import com.kelsos.mbrc.annotations.Repeat.Mode
import com.kelsos.mbrc.common.mvp.BaseView
import com.kelsos.mbrc.common.mvp.Presenter
import com.kelsos.mbrc.events.ui.ShuffleChange.ShuffleState
import com.kelsos.mbrc.events.ui.UpdateDuration

interface PlayerView : BaseView {
  fun updateShuffleState(
    @ShuffleState shuffleState: String,
  )

  fun updateRepeat(
    @Mode mode: String,
  )

  fun updateVolume(
    volume: Int,
    mute: Boolean,
  )

  fun updatePlayState(
    @State state: String,
  )

  fun updateTrackInfo(info: TrackInfo)

  fun updateConnection(status: Int)

  fun updateScrobbleStatus(active: Boolean)

  fun updateLfmStatus(status: LfmStatus)

  fun updateCover(path: String)

  fun updateProgress(duration: UpdateDuration)

  fun showChangeLog()

  fun notifyPluginOutOfDate()

  fun updateDuration(
    position: Int,
    duration: Int,
  )

  fun showPluginUpdateRequired(minimumRequired: String)
}

interface PlayerViewPresenter : Presenter<PlayerView> {
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
