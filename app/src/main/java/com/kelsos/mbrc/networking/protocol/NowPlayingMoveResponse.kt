package com.kelsos.mbrc.networking.protocol

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class NowPlayingMoveResponse(
  @Json(name = "from")
  val from: Int,
  @Json(name = "to")
  val to: Int,
  @Json(name = "success")
  val success: Boolean = false
)
