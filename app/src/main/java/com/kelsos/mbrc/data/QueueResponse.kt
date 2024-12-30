package com.kelsos.mbrc.data

import com.fasterxml.jackson.annotation.JsonProperty

data class QueueResponse(
  @JsonProperty("code") val code: Int,
)
