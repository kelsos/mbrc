package com.kelsos.mbrc.networking.protocol

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
class PageRange {
  @Json(name = "offset")
  var offset: Int = 0

  @Json(name = "limit")
  var limit: Int = 0
}
