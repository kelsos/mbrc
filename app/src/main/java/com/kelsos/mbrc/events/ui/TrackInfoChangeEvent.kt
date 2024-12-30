package com.kelsos.mbrc.events.ui

import com.kelsos.mbrc.domain.TrackInfo

data class TrackInfoChangeEvent(
  val trackInfo: TrackInfo,
)
