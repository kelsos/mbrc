package com.kelsos.mbrc.networking.protocol

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class NowPlayingMoveRequest(
  @Json(name = "from")
  val from: Int,
  @Json(name = "to")
  val to: Int,
)
