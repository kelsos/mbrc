package com.kelsos.mbrc.features.library.albums

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class AlbumCover(
  @Json(name = "artist")
  val artist: String?,
  @Json(name = "album")
  val album: String?,
  @Json(name = "hash")
  val hash: String?
)

data class CachedAlbumCover(val id: Long, val cover: String?)
