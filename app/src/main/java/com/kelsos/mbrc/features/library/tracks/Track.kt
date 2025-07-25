package com.kelsos.mbrc.features.library.tracks

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
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
  val id: Long
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
  val year: String = ""
)

@Entity(tableName = "track", indices = [Index("src", name = "track_src_index", unique = true)])
data class TrackEntity(
  @ColumnInfo
  val artist: String,
  @ColumnInfo
  val title: String,
  @ColumnInfo
  val src: String,
  @ColumnInfo
  val trackno: Int,
  @ColumnInfo
  val disc: Int,
  @ColumnInfo(name = "album_artist")
  val albumArtist: String,
  @ColumnInfo
  val album: String,
  @ColumnInfo
  val genre: String,
  @ColumnInfo
  val year: String,
  @ColumnInfo(name = "sortable_year")
  val sortableYear: String,
  @ColumnInfo(name = "date_added")
  val dateAdded: Long = 0,
  @PrimaryKey(autoGenerate = true)
  val id: Long = 0
)

class TrackPath(val src: String, val id: Long)

fun Track.key(): String = "${albumArtist}_$album".encodeUtf8().sha1().hex().uppercase()
