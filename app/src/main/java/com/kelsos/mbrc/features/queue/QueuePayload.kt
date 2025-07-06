package com.kelsos.mbrc.features.queue

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class QueuePayload(
  @Json(name = "queue")
  val type: String,
  @Json(name = "data")
  val data: List<String>,
  @Json(name = "play")
  val play: String? = null
)
