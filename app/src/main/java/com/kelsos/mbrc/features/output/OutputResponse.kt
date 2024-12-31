package com.kelsos.mbrc.features.output

import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.annotation.JsonPropertyOrder

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder("devices", "active")
data class OutputResponse(
  @field:JsonProperty("devices")
  val devices: List<String> = emptyList(),
  @field:JsonProperty("active")
  val active: String = "",
)
