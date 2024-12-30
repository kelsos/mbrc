package com.kelsos.mbrc.events.ui

import com.kelsos.mbrc.domain.TrackInfo

data class RemoteClientMetaData(
  val trackInfo: TrackInfo,
  val coverPath: String = "",
  val duration: Long,
)
