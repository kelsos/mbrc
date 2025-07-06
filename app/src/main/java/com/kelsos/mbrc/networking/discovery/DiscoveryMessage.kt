package com.kelsos.mbrc.networking.discovery

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class DiscoveryMessage(
  @Json(name = "name")
  val name: String = "",
  @Json(name = "address")
  val address: String = "",
  @Json(name = "port")
  val port: Int = 0,
  @Json(name = "context")
  val context: String = ""
)
