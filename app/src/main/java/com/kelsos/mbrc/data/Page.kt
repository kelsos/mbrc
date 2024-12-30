package com.kelsos.mbrc.data

import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.annotation.JsonPropertyOrder
import java.util.*

@JsonInclude(JsonInclude.Include.NON_NULL)

@JsonPropertyOrder("total", "offset", "limit", "data")
class Page<T> {

  @JsonProperty("total")
  var total: Int = 0

  @JsonProperty("offset")
  var offset: Int = 0

  @JsonProperty("limit")
  var limit: Int = 0

  @JsonProperty("data")
  var data: List<T> = ArrayList()
}
