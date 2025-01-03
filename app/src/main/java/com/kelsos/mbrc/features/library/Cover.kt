package com.kelsos.mbrc.features.library

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class Cover(
  @Json(name = "status")
  val status: Int,
  @Json(name = "cover")
  val cover: String?,
  @Json(name = "hash")
  val hash: String?,
)
