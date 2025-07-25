package com.kelsos.mbrc.data

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

val MIGRATION_3_4 = object : Migration(3, 4) {
  override fun migrate(db: SupportSQLiteDatabase) {
    // Add new columns to existing tables before recreation
    db.execSQL("ALTER TABLE track ADD COLUMN year TEXT NOT NULL DEFAULT ''")
    db.execSQL("ALTER TABLE track ADD COLUMN sortable_year TEXT NOT NULL DEFAULT ''")
    db.execSQL("ALTER TABLE settings ADD COLUMN is_default INTEGER")

    // Remove count columns from tables (if they exist)
    // Note: SQLite doesn't support DROP COLUMN directly, so we need to recreate tables

    // Recreate genre table without count column
    db.execSQL(
      """
      CREATE TABLE genre_new (
        genre TEXT NOT NULL,
        date_added INTEGER NOT NULL DEFAULT 0,
        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL
      )
    """
    )

    db.execSQL(
      """
      INSERT INTO genre_new (genre, date_added, id)
      SELECT
        COALESCE(genre, '') as genre,
        MAX(COALESCE(date_added, 0)) as date_added,
        MIN(id) as id
      FROM genre
      GROUP BY COALESCE(genre, '')
    """
    )

    db.execSQL("DROP TABLE genre")
    db.execSQL("ALTER TABLE genre_new RENAME TO genre")
    db.execSQL("CREATE UNIQUE INDEX genre_genre_idx ON genre (genre)")

    // Recreate artist table without count column
    db.execSQL(
      """
      CREATE TABLE artist_new (
        artist TEXT NOT NULL,
        date_added INTEGER NOT NULL DEFAULT 0,
        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL
      )
    """
    )

    db.execSQL(
      """
      INSERT INTO artist_new (artist, date_added, id)
      SELECT
        COALESCE(artist, '') as artist,
        MAX(COALESCE(date_added, 0)) as date_added,
        MIN(id) as id
      FROM artist
      GROUP BY COALESCE(artist, '')
    """
    )

    db.execSQL("DROP TABLE artist")
    db.execSQL("ALTER TABLE artist_new RENAME TO artist")
    db.execSQL("CREATE UNIQUE INDEX artist_artist_idx ON artist (artist)")

    // Recreate album table without count column
    db.execSQL(
      """
      CREATE TABLE album_new (
        artist TEXT NOT NULL,
        album TEXT NOT NULL,
        cover TEXT,
        date_added INTEGER NOT NULL DEFAULT 0,
        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL
      )
    """
    )

    db.execSQL(
      """
      INSERT INTO album_new (artist, album, cover, date_added, id)
      SELECT
        COALESCE(artist, '') as artist,
        COALESCE(album, '') as album,
        MAX(cover) as cover,
        MAX(COALESCE(date_added, 0)) as date_added,
        MIN(id) as id
      FROM album
      GROUP BY COALESCE(artist, ''), COALESCE(album, '')
    """
    )

    db.execSQL("DROP TABLE album")
    db.execSQL("ALTER TABLE album_new RENAME TO album")
    db.execSQL("CREATE UNIQUE INDEX album_info_idx ON album (artist, album)")

    // Update track table to handle nullability
    db.execSQL(
      """
      CREATE TABLE track_new (
        artist TEXT NOT NULL,
        title TEXT NOT NULL,
        src TEXT NOT NULL,
        trackno INTEGER NOT NULL,
        disc INTEGER NOT NULL,
        album_artist TEXT NOT NULL,
        album TEXT NOT NULL,
        genre TEXT NOT NULL,
        year TEXT NOT NULL,
        sortable_year TEXT NOT NULL,
        date_added INTEGER NOT NULL DEFAULT 0,
        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL
      )
    """
    )

    db.execSQL(
      """
      INSERT INTO track_new (artist, title, src, trackno, disc, album_artist, album, genre, year, sortable_year, date_added, id)
      SELECT
        COALESCE(artist, '') as artist,
        COALESCE(title, '') as title,
        COALESCE(src, '') as src,
        COALESCE(trackno, 0) as trackno,
        COALESCE(disc, 0) as disc,
        COALESCE(album_artist, '') as album_artist,
        COALESCE(album, '') as album,
        COALESCE(genre, '') as genre,
        '' as year,
        '' as sortable_year,
        MAX(COALESCE(date_added, 0)) as date_added,
        MIN(id) as id
      FROM track
      GROUP BY COALESCE(src, '')
    """
    )

    db.execSQL("DROP TABLE track")
    db.execSQL("ALTER TABLE track_new RENAME TO track")
    db.execSQL("CREATE UNIQUE INDEX track_src_index ON track (src)")

    // Update playlists table to handle nullability
    db.execSQL(
      """
      CREATE TABLE playlists_new (
        name TEXT NOT NULL,
        url TEXT NOT NULL,
        date_added INTEGER NOT NULL DEFAULT 0,
        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL
      )
    """
    )

    db.execSQL(
      """
      INSERT INTO playlists_new (name, url, date_added, id)
      SELECT
        COALESCE(name, '') as name,
        MAX(COALESCE(url, '')) as url,
        MAX(COALESCE(date_added, 0)) as date_added,
        MIN(id) as id
      FROM playlists
      GROUP BY COALESCE(name, '')
    """
    )

    db.execSQL("DROP TABLE playlists")
    db.execSQL("ALTER TABLE playlists_new RENAME TO playlists")
    db.execSQL("CREATE UNIQUE INDEX playlist_name_idx ON playlists (name)")

    // Update radio_station table to handle nullability
    db.execSQL(
      """
      CREATE TABLE radio_station_new (
        name TEXT NOT NULL,
        url TEXT NOT NULL,
        date_added INTEGER NOT NULL DEFAULT 0,
        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL
      )
    """
    )

    db.execSQL(
      """
      INSERT INTO radio_station_new (name, url, date_added, id)
      SELECT
        MAX(COALESCE(name, '')) as name,
        COALESCE(url, '') as url,
        MAX(COALESCE(date_added, 0)) as date_added,
        MIN(id) as id
      FROM radio_station
      GROUP BY COALESCE(url, '')
    """
    )

    db.execSQL("DROP TABLE radio_station")
    db.execSQL("ALTER TABLE radio_station_new RENAME TO radio_station")
    db.execSQL("CREATE UNIQUE INDEX radio_url_idx ON radio_station (url)")

    // Update settings table to handle nullability and add is_default
    db.execSQL(
      """
      CREATE TABLE settings_new (
        address TEXT NOT NULL,
        port INTEGER NOT NULL,
        name TEXT NOT NULL,
        is_default INTEGER,
        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL
      )
    """
    )

    db.execSQL(
      """
      INSERT INTO settings_new (address, port, name, is_default, id)
      SELECT
        COALESCE(address, '') as address,
        COALESCE(port, 0) as port,
        MAX(COALESCE(name, '')) as name,
        NULL as is_default,
        MIN(id) as id
      FROM settings
      GROUP BY COALESCE(address, ''), COALESCE(port, 0)
    """
    )

    db.execSQL("DROP TABLE settings")
    db.execSQL("ALTER TABLE settings_new RENAME TO settings")
    db.execSQL("CREATE UNIQUE INDEX index_settings_address_port ON settings (address, port)")
  }
}
