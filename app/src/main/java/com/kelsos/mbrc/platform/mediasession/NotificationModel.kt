package com.kelsos.mbrc.platform.mediasession

import android.graphics.Bitmap
import com.kelsos.mbrc.annotations.PlayerState
import com.kelsos.mbrc.features.player.TrackInfo
import javax.inject.Inject

class NotificationModel
  @Inject
  constructor() {
    var trackInfo: TrackInfo? = null
    var cover: Bitmap? = null

    @PlayerState.State
    var playState: String = PlayerState.STOPPED
  }
