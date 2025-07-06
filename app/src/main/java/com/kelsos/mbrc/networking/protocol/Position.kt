package com.kelsos.mbrc.networking.protocol

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class Position(
  @Json(name = "current")
  val current: Long,
  @Json(name = "total")
  val total: Long
)
