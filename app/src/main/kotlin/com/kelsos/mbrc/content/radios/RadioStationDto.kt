package com.kelsos.mbrc.content.radios

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class RadioStationDto(
  @Json(name="name")
  var name: String = "",
  @Json(name="url")
  var url: String = ""
)