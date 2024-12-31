package com.kelsos.mbrc.networking.protocol

import com.fasterxml.jackson.annotation.JsonProperty

data class NowPlayingMoveRequest(
  @JsonProperty("from") val from: Int,
  @JsonProperty("to") val to: Int,
)
