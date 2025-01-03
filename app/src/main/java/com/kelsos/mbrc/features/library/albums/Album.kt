package com.kelsos.mbrc.features.library.albums

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import okio.ByteString.Companion.encodeUtf8

data class Album(
  val id: Long,
  val artist: String,
  val album: String,
  val cover: String?,
)

@Entity(
  tableName = "album",
  indices = [],
)
data class AlbumEntity(
  @ColumnInfo
  val artist: String? = null,
  @ColumnInfo
  val album: String? = null,
  @ColumnInfo
  val cover: String? = null,
  @ColumnInfo(name = "date_added")
  val dateAdded: Long? = null,
  @ColumnInfo(name = "count")
  val count: Int? = null,
  @PrimaryKey(autoGenerate = true)
  val id: Long? = null,
)

@JsonClass(generateAdapter = true)
data class AlbumDto(
  @Json(name = "artist")
  val artist: String = "",
  @Json(name = "album")
  val album: String = "",
  @Json(name = "count")
  val count: Int = 0,
)

fun Album.key(): String = "${artist}_$album".encodeUtf8().sha1().hex().uppercase()
