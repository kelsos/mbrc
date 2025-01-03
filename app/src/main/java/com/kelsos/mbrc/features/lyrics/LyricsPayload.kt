package com.kelsos.mbrc.features.lyrics

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class LyricsPayload(
  @Json(name = "status")
  val status: Int = NOT_FOUND,
  @Json(name = "lyrics")
  val lyrics: String = "",
) {
  companion object {
    const val SUCCESS = 200
    const val NOT_FOUND = 404
  }
}
