package com.kelsos.mbrc.data

import com.kelsos.mbrc.features.library.Album
import com.kelsos.mbrc.features.library.Artist
import com.kelsos.mbrc.features.library.Genre
import com.kelsos.mbrc.features.library.Track
import com.kelsos.mbrc.features.nowplaying.NowPlaying
import com.kelsos.mbrc.features.playlists.Playlist
import com.raizlabs.android.dbflow.annotation.Migration
import com.raizlabs.android.dbflow.sql.SQLiteType
import com.raizlabs.android.dbflow.sql.migration.AlterTableMigration
import com.raizlabs.android.dbflow.annotation.Database as Db

@Db(version = Database.VERSION, name = Database.NAME)
object Database {
  const val VERSION = 3
  const val NAME = "cache"

  @Migration(version = 3, database = Database::class)
  class Migration3Genre(
    table: Class<Genre>,
  ) : AlterTableMigration<Genre>(table) {
    override fun onPreMigrate() {
      addColumn(SQLiteType.INTEGER, "date_added")
    }
  }

  @Migration(version = 3, database = Database::class)
  class Migration3Artist(
    table: Class<Artist>,
  ) : AlterTableMigration<Artist>(table) {
    override fun onPreMigrate() {
      addColumn(SQLiteType.INTEGER, "date_added")
    }
  }

  @Migration(version = 3, database = Database::class)
  class Migration3Album(
    table: Class<Album>,
  ) : AlterTableMigration<Album>(table) {
    override fun onPreMigrate() {
      addColumn(SQLiteType.TEXT, "cover")
      addColumn(SQLiteType.INTEGER, "date_added")
    }
  }

  @Migration(version = 3, database = Database::class)
  class Migration3Track(
    table: Class<Track>,
  ) : AlterTableMigration<Track>(table) {
    override fun onPreMigrate() {
      addColumn(SQLiteType.INTEGER, "date_added")
    }
  }

  @Migration(version = 3, database = Database::class)
  class Migration3NowPlaying(
    table: Class<NowPlaying>,
  ) : AlterTableMigration<NowPlaying>(table) {
    override fun onPreMigrate() {
      addColumn(SQLiteType.INTEGER, "date_added")
    }
  }

  @Migration(version = 3, database = Database::class)
  class Migration3Playlist(
    table: Class<Playlist>,
  ) : AlterTableMigration<Playlist>(table) {
    override fun onPreMigrate() {
      addColumn(SQLiteType.INTEGER, "date_added")
    }
  }
}
