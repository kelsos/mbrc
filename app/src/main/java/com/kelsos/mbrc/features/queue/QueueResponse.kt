package com.kelsos.mbrc.features.queue

import com.fasterxml.jackson.annotation.JsonProperty

data class QueueResponse(
  @JsonProperty("code") val code: Int,
)
