package com.kelsos.mbrc.features.nowplaying

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

data class NowPlaying(
  val title: String,
  val artist: String,
  val path: String,
  val position: Int,
  val id: Long,
)

@JsonClass(generateAdapter = true)
data class NowPlayingDto(
  @Json(name = "title")
  val title: String = "",
  @Json(name = "artist")
  val artist: String = "",
  @Json(name = "path")
  val path: String = "",
  @Json(name = "position")
  val position: Int = 0,
)

@Entity(
  tableName = "now_playing",
  indices = [],
)
data class NowPlayingEntity(
  @ColumnInfo(name = "title")
  val title: String? = null,
  @ColumnInfo(name = "artist")
  val artist: String? = null,
  @ColumnInfo(name = "path")
  val path: String? = null,
  @ColumnInfo(name = "position")
  val position: Int? = null,
  @ColumnInfo(name = "date_added")
  val dateAdded: Long? = null,
  @PrimaryKey(autoGenerate = true)
  val id: Long? = null,
)

data class CachedNowPlaying(
  val id: Long,
  val path: String,
  val position: Int,
)
