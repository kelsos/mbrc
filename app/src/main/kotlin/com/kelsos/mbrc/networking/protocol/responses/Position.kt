package com.kelsos.mbrc.networking.protocol.responses

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class Position(
  @Json(name = "current")
  val current: Int,
  @Json(name = "total")
  val total: Int
)