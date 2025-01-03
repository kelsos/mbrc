package com.kelsos.mbrc.networking.protocol

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
class Page<T> {
  @Json(name = "total")
  var total: Int = 0

  @Json(name = "offset")
  var offset: Int = 0

  @Json(name = "limit")
  var limit: Int = 0

  @Json(name = "data")
  var data: List<T> = ArrayList()
}
