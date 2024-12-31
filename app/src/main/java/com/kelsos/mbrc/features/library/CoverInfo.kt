package com.kelsos.mbrc.features.library

import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.JsonProperty

@JsonInclude(JsonInclude.Include.NON_NULL)
data class CoverInfo(
  @JsonProperty("artist")
  val artist: String,
  @JsonProperty("album")
  val album: String,
  @JsonProperty("hash")
  val hash: String,
)
