package com.kelsos.mbrc.data

import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.JsonProperty
import com.kelsos.mbrc.utilities.RemoteUtils

@JsonInclude(JsonInclude.Include.NON_NULL)
data class CoverInfo(
  @JsonProperty("artist")
  val artist: String,
  @JsonProperty("album")
  val album: String,
  @JsonProperty("hash")
  val hash: String
)

fun CoverInfo.key(): String = RemoteUtils.sha1("${artist}_${album}")
