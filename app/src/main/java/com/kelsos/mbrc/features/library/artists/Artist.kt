package com.kelsos.mbrc.features.library.artists

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

data class Artist(val artist: String, val id: Long)

@JsonClass(generateAdapter = true)
data class ArtistDto(
  @Json(name = "artist")
  val artist: String = "",
  @Json(name = "count")
  val count: Int = 0
)

@Entity(
  tableName = "artist",
  indices = []
)
data class ArtistEntity(
  @ColumnInfo
  val artist: String? = null,
  @ColumnInfo
  val count: Int? = null,
  @ColumnInfo(name = "date_added")
  val dateAdded: Long? = null,
  @PrimaryKey(autoGenerate = true)
  val id: Long? = null
)
