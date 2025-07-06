package com.kelsos.mbrc.features.player

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class CoverPayload(
  @Json(name = "status")
  val status: Int = NOT_FOUND,
  @Json(name = "cover")
  val cover: String = ""
) {
  companion object {
    const val READY = 1
    const val SUCCESS = 200
    const val NOT_FOUND = 404
  }
}
