package com.kelsos.mbrc.data

import com.fasterxml.jackson.annotation.JsonProperty

data class LyricsPayload(
  @JsonProperty("status") val status: Int = NOT_FOUND,
  @JsonProperty("lyrics") val lyrics: String = ""
) {

  companion object {
    const val SUCCESS = 200
    const val NOT_FOUND = 404
  }
}

