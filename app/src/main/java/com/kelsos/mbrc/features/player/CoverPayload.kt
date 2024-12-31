package com.kelsos.mbrc.features.player

import com.fasterxml.jackson.annotation.JsonProperty

data class CoverPayload(
  @JsonProperty("status") val status: Int = NOT_FOUND,
  @JsonProperty("cover") val cover: String = "",
) {
  companion object {
    const val READY = 1
    const val SUCCESS = 200
    const val NOT_FOUND = 404
  }
}
