package com.kelsos.mbrc.events.ui

import com.kelsos.mbrc.features.player.TrackInfo

data class TrackInfoChangeEvent(
  val trackInfo: TrackInfo,
)
