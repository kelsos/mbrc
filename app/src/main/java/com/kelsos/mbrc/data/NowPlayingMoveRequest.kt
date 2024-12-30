package com.kelsos.mbrc.data

import com.fasterxml.jackson.annotation.JsonProperty

data class NowPlayingMoveRequest(
  @JsonProperty("from") val from: Int,
  @JsonProperty("to") val to: Int,
)
