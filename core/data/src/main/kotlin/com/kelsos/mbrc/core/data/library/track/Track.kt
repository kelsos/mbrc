package com.kelsos.mbrc.core.data.library.track

import androidx.compose.runtime.Immutable
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import okio.ByteString.Companion.encodeUtf8

@Immutable
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

/** Minimal projection used to derive the dimension tables and tag junctions from tracks. */
data class TrackTagRow(
  val id: Long,
  val artist: String,
  @ColumnInfo(name = "album_artist")
  val albumArtist: String,
  val genre: String
)

fun Track.key(): String = "${albumArtist}_$album".encodeUtf8().sha1().hex().uppercase()
