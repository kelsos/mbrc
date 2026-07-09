package com.kelsos.mbrc.core.data.library.junction

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index

/**
 * Junction relating a track to each of its (split) artists.
 *
 * MusicBee's artist lookup does not split multi-artist values, so both the
 * track's `artist`/`album_artist` fields and the artist browse rows can arrive
 * as a compound "Artist1;Artist2" string. This table links a track to every
 * individual artist it belongs to. [isAlbumArtist] distinguishes the two roles
 * so album-artist browsing stays separate from track-artist browsing.
 */
@Entity(
  tableName = "track_artist",
  primaryKeys = ["track_id", "artist_id", "is_album_artist"],
  indices = [Index("artist_id")]
)
data class TrackArtistEntity(
  @ColumnInfo(name = "track_id")
  val trackId: Long,
  @ColumnInfo(name = "artist_id")
  val artistId: Long,
  @ColumnInfo(name = "is_album_artist")
  val isAlbumArtist: Int
)
