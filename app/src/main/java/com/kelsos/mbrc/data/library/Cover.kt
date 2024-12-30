package com.kelsos.mbrc.data.library

import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.JsonProperty

@JsonInclude(JsonInclude.Include.NON_NULL)
data class Cover(
  @JsonProperty("status")
  val status: Int,
  @JsonProperty("cover")
  val cover: String?,
  @JsonProperty("hash")
  val hash: String?,
)
