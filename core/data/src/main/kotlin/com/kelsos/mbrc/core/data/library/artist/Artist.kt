package com.kelsos.mbrc.core.data.library.artist

import androidx.compose.runtime.Immutable
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Immutable
data class Artist(val artist: String, val id: Long)

@Entity(
  tableName = "artist",
  indices = [Index("artist", name = "artist_artist_idx", unique = true)]
)
data class ArtistEntity(
  @ColumnInfo(collate = ColumnInfo.NOCASE)
  val artist: String,
  @ColumnInfo(name = "date_added")
  val dateAdded: Long = 0,
  @PrimaryKey(autoGenerate = true)
  val id: Long = 0
)
