package com.kelsos.mbrc.platform.mediasession

import android.graphics.Bitmap
import com.kelsos.mbrc.common.state.PlayerState
import com.kelsos.mbrc.common.state.PlayingTrack

data class NotificationData(
  val track: PlayingTrack = PlayingTrack(),
  val playerState: PlayerState = PlayerState.Stopped,
  val cover: Bitmap? = null
)
