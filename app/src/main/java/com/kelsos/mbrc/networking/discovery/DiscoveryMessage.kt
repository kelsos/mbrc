package com.kelsos.mbrc.networking.discovery

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty

@JsonIgnoreProperties(ignoreUnknown = true)
class DiscoveryMessage {
  @JsonProperty("name")
  var name: String? = null

  @JsonProperty("address")
  var address: String? = null

  @JsonProperty("port")
  var port: Int = 0

  @JsonProperty("context")
  var context: String? = null

  override fun toString(): String = "{name='$name', address='$address', port=$port, context='$context'}"
}
