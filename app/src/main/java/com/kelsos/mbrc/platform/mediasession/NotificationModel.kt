package com.kelsos.mbrc.platform.mediasession

import android.graphics.Bitmap
import com.kelsos.mbrc.annotations.PlayerState
import com.kelsos.mbrc.features.player.TrackInfo

class NotificationModel {
  var trackInfo: TrackInfo? = null
  var cover: Bitmap? = null

  @PlayerState.State
  var playState: String = PlayerState.STOPPED
}
