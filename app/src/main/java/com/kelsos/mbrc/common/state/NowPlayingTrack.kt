package com.kelsos.mbrc.common.state

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class NowPlayingTrack(
  @Json(name = "artist")
  val artist: String,
  @Json(name = "album")
  val album: String,
  @Json(name = "title")
  val title: String,
  @Json(name = "year")
  val year: String,
  @Json(name = "path")
  val path: String,
)
