package com.kelsos.mbrc.networking.protocol

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class ProtocolPayload(
  @Json(name = "client_id")
  val clientId: String,
  @Json(name = "no_broadcast")
  val noBroadcast: Boolean = false,
  @Json(name = "protocol_version")
  val protocolVersion: Int = 3
)
