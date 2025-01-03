package com.kelsos.mbrc.features.library.tracks

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import okio.ByteString.Companion.encodeUtf8

data class Track(
  val artist: String,
  val title: String,
  val src: String,
  val trackno: Int,
  val disc: Int,
  val albumArtist: String,
  val album: String,
  val genre: String,
  val year: String,
  val id: Long,
)

@JsonClass(generateAdapter = true)
data class TrackDto(
  @Json(name = "artist")
  val artist: String = "",
  @Json(name = "title")
  val title: String = "",
  @Json(name = "src")
  val src: String = "",
  @Json(name = "trackno")
  val trackno: Int = 0,
  @Json(name = "disc")
  val disc: Int = 0,
  @Json(name = "album_artist")
  val albumArtist: String = "",
  @Json(name = "album")
  val album: String = "",
  @Json(name = "genre")
  val genre: String = "",
  @Json(name = "year")
  val year: String = "",
)

@Entity(tableName = "track", indices = [])
data class TrackEntity(
  @ColumnInfo
  val artist: String? = null,
  @ColumnInfo
  val title: String? = null,
  @ColumnInfo
  val src: String? = null,
  @ColumnInfo
  val trackno: Int? = null,
  @ColumnInfo
  val disc: Int? = null,
  @ColumnInfo(name = "album_artist")
  val albumArtist: String? = null,
  @ColumnInfo
  val album: String? = null,
  @ColumnInfo
  val genre: String? = null,
  @ColumnInfo(name = "date_added")
  val dateAdded: Long? = null,
  @PrimaryKey(autoGenerate = true)
  val id: Long? = null,
)

class TrackPath(
  val src: String,
  val id: Long,
)

fun Track.key(): String = "${albumArtist}_$album".encodeUtf8().sha1().hex().uppercase()
