package com.kelsos.mbrc.model

import android.graphics.Bitmap
import com.kelsos.mbrc.annotations.PlayerState
import com.kelsos.mbrc.annotations.PlayerState.State
import com.kelsos.mbrc.domain.TrackInfo
import javax.inject.Inject

class NotificationModel
  @Inject
  constructor() {
    var trackInfo: TrackInfo? = null
    var cover: Bitmap? = null

    @State
    var playState: String = PlayerState.STOPPED
  }
