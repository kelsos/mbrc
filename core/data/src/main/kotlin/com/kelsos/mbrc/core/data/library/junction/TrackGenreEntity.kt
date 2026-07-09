package com.kelsos.mbrc.core.data.library.junction

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index

/**
 * Junction relating a track to each of its (split) genres.
 *
 * MusicBee stores a track's genre as a compound "Rock; Electronic" string while
 * the genre browse list is already split into individual rows. This table links
 * a track to every genre row it belongs to so navigation stops missing
 * multi-genre tracks.
 */
@Entity(
  tableName = "track_genre",
  primaryKeys = ["track_id", "genre_id"],
  indices = [Index("genre_id")]
)
data class TrackGenreEntity(
  @ColumnInfo(name = "track_id")
  val trackId: Long,
  @ColumnInfo(name = "genre_id")
  val genreId: Long
)
