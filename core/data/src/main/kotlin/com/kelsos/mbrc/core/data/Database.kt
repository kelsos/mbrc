package com.kelsos.mbrc.core.data

import androidx.room.Database
import androidx.room.RoomDatabase
import com.kelsos.mbrc.core.data.Database.Companion.VERSION
import com.kelsos.mbrc.core.data.library.album.AlbumDao
import com.kelsos.mbrc.core.data.library.album.AlbumEntity
import com.kelsos.mbrc.core.data.library.artist.ArtistDao
import com.kelsos.mbrc.core.data.library.artist.ArtistEntity
import com.kelsos.mbrc.core.data.library.genre.GenreDao
import com.kelsos.mbrc.core.data.library.genre.GenreEntity
import com.kelsos.mbrc.core.data.library.junction.TrackArtistDao
import com.kelsos.mbrc.core.data.library.junction.TrackArtistEntity
import com.kelsos.mbrc.core.data.library.junction.TrackGenreDao
import com.kelsos.mbrc.core.data.library.junction.TrackGenreEntity
import com.kelsos.mbrc.core.data.library.track.TrackDao
import com.kelsos.mbrc.core.data.library.track.TrackEntity
import com.kelsos.mbrc.core.data.nowplaying.NowPlayingDao
import com.kelsos.mbrc.core.data.nowplaying.NowPlayingEntity
import com.kelsos.mbrc.core.data.playlist.PlaylistDao
import com.kelsos.mbrc.core.data.playlist.PlaylistEntity
import com.kelsos.mbrc.core.data.radio.RadioStationDao
import com.kelsos.mbrc.core.data.radio.RadioStationEntity
import com.kelsos.mbrc.core.data.settings.ConnectionDao
import com.kelsos.mbrc.core.data.settings.ConnectionSettingsEntity

@Database(
  entities = [
    GenreEntity::class,
    ArtistEntity::class,
    AlbumEntity::class,
    TrackEntity::class,
    TrackGenreEntity::class,
    TrackArtistEntity::class,
    NowPlayingEntity::class,
    PlaylistEntity::class,
    RadioStationEntity::class,
    ConnectionSettingsEntity::class
  ],
  version = VERSION
)
abstract class Database : RoomDatabase() {
  abstract fun genreDao(): GenreDao

  abstract fun artistDao(): ArtistDao

  abstract fun albumDao(): AlbumDao

  abstract fun trackDao(): TrackDao

  abstract fun trackGenreDao(): TrackGenreDao

  abstract fun trackArtistDao(): TrackArtistDao

  abstract fun nowPlayingDao(): NowPlayingDao

  abstract fun playlistDao(): PlaylistDao

  abstract fun radioStationDao(): RadioStationDao

  abstract fun connectionDao(): ConnectionDao

  companion object {
    const val VERSION = 5
    const val NAME = "cache.db"
  }
}
