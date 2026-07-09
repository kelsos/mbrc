package com.kelsos.mbrc.core.data

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

// Column order of the rescue query in readLegacyConnections.
private const val COLUMN_ADDRESS = 0
private const val COLUMN_PORT = 1
private const val COLUMN_NAME = 2
private const val COLUMN_ID = 3

/**
 * A saved connection carried across the rebuild of a pre-Room database.
 *
 * [id] is preserved deliberately: `DefaultConnectionMigration` resolves the user's default
 * connection by row id out of SharedPreferences, so reassigning ids here would silently point that
 * lookup at the wrong server.
 */
private data class LegacyConnection(
  val id: Long,
  val address: String,
  val port: Int,
  val name: String
)

/**
 * Tables that may exist in a pre-Room database. Dropped and rebuilt empty, except for `settings`
 * whose rows are carried over.
 */
private val LEGACY_TABLES = listOf(
  "genre",
  "artist",
  "album",
  "track",
  "now_playing",
  "playlists",
  "radio_station",
  "settings"
)

/** The version 4 schema, verbatim from `schemas/com.kelsos.mbrc.core.data.Database/4.json`. */
private val SCHEMA_V4 = listOf(
  "CREATE TABLE IF NOT EXISTS `genre` (`genre` TEXT NOT NULL, `date_added` INTEGER NOT NULL, " +
    "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL)",
  "CREATE UNIQUE INDEX IF NOT EXISTS `genre_genre_idx` ON `genre` (`genre`)",
  "CREATE TABLE IF NOT EXISTS `artist` (`artist` TEXT NOT NULL, `date_added` INTEGER NOT NULL, " +
    "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL)",
  "CREATE UNIQUE INDEX IF NOT EXISTS `artist_artist_idx` ON `artist` (`artist`)",
  "CREATE TABLE IF NOT EXISTS `album` (`artist` TEXT NOT NULL, `album` TEXT NOT NULL, " +
    "`cover` TEXT, `date_added` INTEGER NOT NULL, " +
    "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL)",
  "CREATE UNIQUE INDEX IF NOT EXISTS `album_info_idx` ON `album` (`artist`, `album`)",
  "CREATE TABLE IF NOT EXISTS `track` (`artist` TEXT NOT NULL, `title` TEXT NOT NULL, " +
    "`src` TEXT NOT NULL, `trackno` INTEGER NOT NULL, `disc` INTEGER NOT NULL, " +
    "`album_artist` TEXT NOT NULL, `album` TEXT NOT NULL, `genre` TEXT NOT NULL, " +
    "`year` TEXT NOT NULL, `sortable_year` TEXT NOT NULL, `date_added` INTEGER NOT NULL, " +
    "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL)",
  "CREATE UNIQUE INDEX IF NOT EXISTS `track_src_index` ON `track` (`src`)",
  "CREATE TABLE IF NOT EXISTS `now_playing` (`title` TEXT NOT NULL, `artist` TEXT NOT NULL, " +
    "`path` TEXT NOT NULL, `position` INTEGER NOT NULL, `date_added` INTEGER NOT NULL, " +
    "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL)",
  "CREATE INDEX IF NOT EXISTS `now_playing_position_idx` ON `now_playing` (`position`)",
  "CREATE INDEX IF NOT EXISTS `now_playing_date_added_idx` ON `now_playing` (`date_added`)",
  "CREATE TABLE IF NOT EXISTS `playlists` (`name` TEXT NOT NULL, `url` TEXT NOT NULL, " +
    "`date_added` INTEGER NOT NULL, `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL)",
  "CREATE UNIQUE INDEX IF NOT EXISTS `playlist_name_idx` ON `playlists` (`name`)",
  "CREATE TABLE IF NOT EXISTS `radio_station` (`name` TEXT NOT NULL, `url` TEXT NOT NULL, " +
    "`date_added` INTEGER NOT NULL, `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL)",
  "CREATE UNIQUE INDEX IF NOT EXISTS `radio_url_idx` ON `radio_station` (`url`)",
  "CREATE TABLE IF NOT EXISTS `settings` (`address` TEXT NOT NULL, `port` INTEGER NOT NULL, " +
    "`name` TEXT NOT NULL, `is_default` INTEGER, " +
    "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL)",
  "CREATE UNIQUE INDEX IF NOT EXISTS `index_settings_address_port` ON `settings` " +
    "(`address`, `port`)"
)

/**
 * Rebuilds a pre-Room database into the version 4 schema, keeping the user's saved connections.
 *
 * Versions 1 and 2 predate the move from DBFlow to Room (Dec 2017) — the first Room database was
 * already version 4 — so these files were written by a library whose table shapes were never
 * exported as a Room schema. There is nothing to validate a hand-written column-by-column
 * migration against, and no `1.json`/`2.json` has ever existed in the repo.
 *
 * What can be salvaged is the part the user cannot regenerate. DBFlow wrote to the same `cache.db`
 * file and its `settings` table has carried the same (address, port, name, id) shape ever since,
 * so the connections are read out, every cache table is dropped and recreated empty at the v4
 * schema, and the connections are written back. The library and queue caches simply refill on the
 * next sync.
 *
 * Without this, Room finds no path from 1 to 4 and throws on first database access, crashing the
 * app on every launch with no way out but clearing app data (#342).
 */
internal fun SupportSQLiteDatabase.rebuildFromLegacySchema() {
  val connections = readLegacyConnections()

  for (table in LEGACY_TABLES) {
    execSQL("DROP TABLE IF EXISTS `$table`")
  }
  for (statement in SCHEMA_V4) {
    execSQL(statement)
  }

  // OR IGNORE because v4 puts a unique index on (address, port) that the legacy table did not
  // have: DBFlow declared the pair as a unique group with ConflictAction.IGNORE, and a unique
  // index does not constrain NULLs, so two rows can arrive that both coalesce to the same pair
  // (most plausibly a pair of half-written rows with no address). Only the first is kept. Such a
  // row cannot describe a reachable server anyway, so the alternative — failing the migration and
  // leaving the user on a crashing app — would be a far worse trade.
  for (connection in connections) {
    execSQL(
      "INSERT OR IGNORE INTO `settings` (`address`, `port`, `name`, `is_default`, `id`) " +
        "VALUES (?, ?, ?, NULL, ?)",
      arrayOf<Any>(connection.address, connection.port, connection.name, connection.id)
    )
  }
}

/**
 * Reads the saved connections out of a pre-Room `settings` table, or returns nothing when the
 * table is absent. Columns were nullable back then, so every value is coalesced to the non-null
 * form v4 requires.
 */
private fun SupportSQLiteDatabase.readLegacyConnections(): List<LegacyConnection> {
  if (!hasTable("settings")) {
    return emptyList()
  }
  val connections = mutableListOf<LegacyConnection>()
  query(
    "SELECT COALESCE(`address`, '') AS address, COALESCE(`port`, 0) AS port, " +
      "COALESCE(`name`, '') AS name, `id` FROM `settings`"
  ).use { cursor ->
    while (cursor.moveToNext()) {
      connections.add(
        LegacyConnection(
          address = cursor.getString(COLUMN_ADDRESS),
          port = cursor.getInt(COLUMN_PORT),
          name = cursor.getString(COLUMN_NAME),
          id = cursor.getLong(COLUMN_ID)
        )
      )
    }
  }
  return connections
}

private fun SupportSQLiteDatabase.hasTable(name: String): Boolean = query(
  "SELECT 1 FROM `sqlite_master` WHERE `type` = 'table' AND `name` = ?",
  arrayOf(name)
).use { cursor -> cursor.moveToFirst() }

/**
 * Pre-Room databases (DBFlow, version 1 and 2) rebuilt onto the v4 schema.
 * See [rebuildFromLegacySchema].
 */
val MIGRATION_1_4 = object : Migration(1, 4) {
  override fun migrate(db: SupportSQLiteDatabase) {
    db.rebuildFromLegacySchema()
  }
}

/** @see MIGRATION_1_4 */
val MIGRATION_2_4 = object : Migration(2, 4) {
  override fun migrate(db: SupportSQLiteDatabase) {
    db.rebuildFromLegacySchema()
  }
}

/**
 * Rebuilds `genre` and `artist` with a `NOCASE` name column, folding rows that differ only by case.
 *
 * Dimension names used to come from the plugin's canonical browse lists, which de-duplicated for
 * us. They are now derived from raw track tags, where "Rock" and "rock" are both common, and the v4
 * unique indices collate BINARY, so both would be kept and shown as separate entries. The lowest id
 * of each group survives so existing junction rows keep pointing at a live dimension.
 */
private fun SupportSQLiteDatabase.recreateWithCaseInsensitiveName(
  table: String,
  column: String,
  index: String
) {
  execSQL(
    """
    CREATE TABLE ${table}_new (
      $column TEXT NOT NULL COLLATE NOCASE,
      date_added INTEGER NOT NULL,
      id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL
    )
    """
  )
  execSQL(
    """
    INSERT INTO ${table}_new ($column, date_added, id)
    SELECT $column, MAX(date_added), MIN(id) FROM $table GROUP BY $column COLLATE NOCASE
    """
  )
  execSQL("DROP TABLE $table")
  execSQL("ALTER TABLE ${table}_new RENAME TO $table")
  execSQL("CREATE UNIQUE INDEX IF NOT EXISTS $index ON $table ($column)")
}

/**
 * Adds the multi-value tag junction tables. They are created empty here; existing
 * installs are backfilled from the compound `track.genre`/`track.artist` columns
 * on library open (LibraryDerivationUseCase.ensureDerived), and every sync rebuilds
 * them going forward.
 *
 * Also folds the genre and artist name columns to `NOCASE`; see
 * [recreateWithCaseInsensitiveName].
 */
val MIGRATION_4_5 = object : Migration(4, 5) {
  override fun migrate(db: SupportSQLiteDatabase) {
    db.recreateWithCaseInsensitiveName("genre", "genre", "genre_genre_idx")
    db.recreateWithCaseInsensitiveName("artist", "artist", "artist_artist_idx")

    db.execSQL(
      """
      CREATE TABLE IF NOT EXISTS track_genre (
        track_id INTEGER NOT NULL,
        genre_id INTEGER NOT NULL,
        PRIMARY KEY(track_id, genre_id)
      )
      """
    )
    db.execSQL("CREATE INDEX IF NOT EXISTS index_track_genre_genre_id ON track_genre (genre_id)")

    db.execSQL(
      """
      CREATE TABLE IF NOT EXISTS track_artist (
        track_id INTEGER NOT NULL,
        artist_id INTEGER NOT NULL,
        is_album_artist INTEGER NOT NULL,
        PRIMARY KEY(track_id, artist_id, is_album_artist)
      )
      """
    )
    db.execSQL(
      "CREATE INDEX IF NOT EXISTS index_track_artist_artist_id ON track_artist (artist_id)"
    )
  }
}

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

    // Recreate now_playing table with non-nullable columns and indexes
    db.execSQL(
      """
      CREATE TABLE now_playing_new (
        title TEXT NOT NULL DEFAULT '',
        artist TEXT NOT NULL DEFAULT '',
        path TEXT NOT NULL DEFAULT '',
        position INTEGER NOT NULL DEFAULT 0,
        date_added INTEGER NOT NULL DEFAULT 0,
        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL
      )
    """
    )

    db.execSQL(
      """
      INSERT INTO now_playing_new (title, artist, path, position, date_added, id)
      SELECT
        COALESCE(title, '') as title,
        COALESCE(artist, '') as artist,
        COALESCE(path, '') as path,
        COALESCE(position, 0) as position,
        COALESCE(date_added, 0) as date_added,
        id
      FROM now_playing
    """
    )

    db.execSQL("DROP TABLE now_playing")
    db.execSQL("ALTER TABLE now_playing_new RENAME TO now_playing")
    db.execSQL("CREATE INDEX now_playing_position_idx ON now_playing (position)")
    db.execSQL("CREATE INDEX now_playing_date_added_idx ON now_playing (date_added)")
  }
}
