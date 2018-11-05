package com.kelsos.mbrc.networking.protocol

import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.annotation.JsonPropertyOrder

@JsonInclude(JsonInclude.Include.NON_NULL)

@JsonPropertyOrder(
  "client_id",
  "no_broadcast",
  "protocol_version"
)
data class ProtocolPayload(
  @JsonProperty("client_id")
  var clientId: String,
  @JsonProperty("no_broadcast")
  var noBroadcast: Boolean = false,
  @JsonProperty("protocol_version")
  var protocolVersion: Int = 3
)