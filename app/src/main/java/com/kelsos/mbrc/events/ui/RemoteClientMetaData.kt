package com.kelsos.mbrc.events.ui

import com.kelsos.mbrc.features.player.TrackInfo

data class RemoteClientMetaData(
  val trackInfo: TrackInfo,
  val coverPath: String = "",
  val duration: Long,
)
