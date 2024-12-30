package com.kelsos.mbrc.data

import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.annotation.JsonPropertyOrder

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder("offset", "limit")
class PageRange {
  @JsonProperty("offset")
  var offset: Int = 0

  @JsonProperty("limit")
  var limit: Int = 0
}
