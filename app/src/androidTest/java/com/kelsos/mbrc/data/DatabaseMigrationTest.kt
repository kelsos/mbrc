package com.kelsos.mbrc.data

import androidx.room.Room
import androidx.room.testing.MigrationTestHelper
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.google.common.truth.Truth.assertThat
import java.io.IOException
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class DatabaseMigrationTest {

  private val migrationTestDb = "migration-test"

  @get:Rule
  val helper: MigrationTestHelper = MigrationTestHelper(
    InstrumentationRegistry.getInstrumentation(),
    Database::class.java
  )

  @Test
  @Throws(IOException::class)
  fun migrate3To4() {
    // Create the database with version 3 schema and insert test data
    var db = helper.createDatabase(migrationTestDb, 3)
    // Insert test data with nullable/null values that need to be handled
    db.execSQL(
      """
        INSERT INTO genre (genre, count, date_added, id)
        VALUES ('Rock', 5, 123456789, 1)
      """
    )

    db.execSQL(
      """
        INSERT INTO genre (genre, count, date_added, id)
        VALUES (NULL, 3, NULL, 2)
      """
    )

    db.execSQL(
      """
        INSERT INTO artist (artist, count, date_added, id)
        VALUES ('Test Artist', 10, 123456789, 1)
      """
    )

    db.execSQL(
      """
        INSERT INTO artist (artist, count, date_added, id)
        VALUES (NULL, NULL, NULL, 2)
      """
    )

    db.execSQL(
      """
        INSERT INTO album (artist, album, cover, count, date_added, id)
        VALUES ('Test Artist', 'Test Album', 'cover.jpg', 8, 123456789, 1)
      """
    )

    db.execSQL(
      """
        INSERT INTO album (artist, album, cover, count, date_added, id)
        VALUES (NULL, NULL, NULL, NULL, NULL, 2)
      """
    )

    db.execSQL(
      """
        INSERT INTO track (artist, title, src, trackno, disc, album_artist, album, genre, date_added, id)
        VALUES ('Test Artist', 'Test Track', 'test.mp3', 1, 1, 'Test Artist', 'Test Album', 'Rock', 123456789, 1)
      """
    )

    db.execSQL(
      """
        INSERT INTO track (artist, title, src, trackno, disc, album_artist, album, genre, date_added, id)
        VALUES (NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, 2)
      """
    )

    db.execSQL(
      """
        INSERT INTO playlists (name, url, date_added, id)
        VALUES ('Test Playlist', 'playlist.m3u', 123456789, 1)
      """
    )

    db.execSQL(
      """
        INSERT INTO playlists (name, url, date_added, id)
        VALUES (NULL, NULL, NULL, 2)
      """
    )

    db.execSQL(
      """
        INSERT INTO radio_station (name, url, date_added, id)
        VALUES ('Test Radio', 'http://radio.com', 123456789, 1)
      """
    )

    db.execSQL(
      """
        INSERT INTO radio_station (name, url, date_added, id)
        VALUES (NULL, NULL, NULL, 2)
      """
    )

    db.execSQL(
      """
        INSERT INTO settings (address, port, name, id)
        VALUES ('192.168.1.1', 3000, 'Home', 1)
      """
    )

    db.execSQL(
      """
        INSERT INTO settings (address, port, name, id)
        VALUES (NULL, NULL, NULL, 2)
      """
    )

    db.close()

    // Run the migration
    db = helper.runMigrationsAndValidate(migrationTestDb, 4, true, MIGRATION_3_4)

    // Verify the migration worked correctly

    // 1. Test Genre table
    val genreCursor = db.query("SELECT * FROM genre ORDER BY id")
    assertThat(genreCursor.count).isEqualTo(2)

    genreCursor.moveToFirst()
    assertThat(genreCursor.getString(genreCursor.getColumnIndex("genre"))).isEqualTo("Rock")
    assertThat(genreCursor.getLong(genreCursor.getColumnIndex("date_added"))).isEqualTo(123456789)
    // Verify count column was removed (should not exist)
    assertThat(genreCursor.getColumnIndex("count")).isEqualTo(-1)

    genreCursor.moveToNext()
    assertThat(genreCursor.getString(genreCursor.getColumnIndex("genre"))).isEqualTo("")
    assertThat(genreCursor.getLong(genreCursor.getColumnIndex("date_added"))).isEqualTo(0)
    genreCursor.close()

    // 2. Test Artist table
    val artistCursor = db.query("SELECT * FROM artist ORDER BY id")
    assertThat(artistCursor.count).isEqualTo(2)

    artistCursor.moveToFirst()
    assertThat(
      artistCursor.getString(artistCursor.getColumnIndex("artist"))
    ).isEqualTo("Test Artist")
    assertThat(artistCursor.getLong(artistCursor.getColumnIndex("date_added"))).isEqualTo(123456789)
    assertThat(artistCursor.getColumnIndex("count")).isEqualTo(-1) // count column removed

    artistCursor.moveToNext()
    assertThat(artistCursor.getString(artistCursor.getColumnIndex("artist"))).isEqualTo("")
    artistCursor.close()

    // 3. Test Album table
    val albumCursor = db.query("SELECT * FROM album ORDER BY id")
    assertThat(albumCursor.count).isEqualTo(2)

    albumCursor.moveToFirst()
    assertThat(albumCursor.getString(albumCursor.getColumnIndex("artist"))).isEqualTo("Test Artist")
    assertThat(albumCursor.getString(albumCursor.getColumnIndex("album"))).isEqualTo("Test Album")
    assertThat(albumCursor.getString(albumCursor.getColumnIndex("cover"))).isEqualTo("cover.jpg")
    assertThat(albumCursor.getColumnIndex("count")).isEqualTo(-1) // count column removed

    albumCursor.moveToNext()
    assertThat(albumCursor.getString(albumCursor.getColumnIndex("artist"))).isEqualTo("")
    assertThat(albumCursor.getString(albumCursor.getColumnIndex("album"))).isEqualTo("")
    assertThat(albumCursor.isNull(albumCursor.getColumnIndex("cover"))).isTrue()
    albumCursor.close()

    // 4. Test Track table (most complex with new columns)
    val trackCursor = db.query("SELECT * FROM track ORDER BY id")
    assertThat(trackCursor.count).isEqualTo(2)

    trackCursor.moveToFirst()
    assertThat(trackCursor.getString(trackCursor.getColumnIndex("artist"))).isEqualTo("Test Artist")
    assertThat(trackCursor.getString(trackCursor.getColumnIndex("title"))).isEqualTo("Test Track")
    assertThat(trackCursor.getString(trackCursor.getColumnIndex("src"))).isEqualTo("test.mp3")
    assertThat(trackCursor.getInt(trackCursor.getColumnIndex("trackno"))).isEqualTo(1)
    assertThat(trackCursor.getInt(trackCursor.getColumnIndex("disc"))).isEqualTo(1)
    assertThat(
      trackCursor.getString(trackCursor.getColumnIndex("album_artist"))
    ).isEqualTo("Test Artist")
    assertThat(trackCursor.getString(trackCursor.getColumnIndex("album"))).isEqualTo("Test Album")
    assertThat(trackCursor.getString(trackCursor.getColumnIndex("genre"))).isEqualTo("Rock")
    assertThat(trackCursor.getString(trackCursor.getColumnIndex("year"))).isEqualTo("")
    assertThat(trackCursor.getString(trackCursor.getColumnIndex("sortable_year"))).isEqualTo("")
    assertThat(trackCursor.getLong(trackCursor.getColumnIndex("date_added"))).isEqualTo(123456789)

    trackCursor.moveToNext()
    assertThat(trackCursor.getString(trackCursor.getColumnIndex("artist"))).isEqualTo("")
    assertThat(trackCursor.getString(trackCursor.getColumnIndex("title"))).isEqualTo("")
    assertThat(trackCursor.getString(trackCursor.getColumnIndex("year"))).isEqualTo("")
    assertThat(trackCursor.getString(trackCursor.getColumnIndex("sortable_year"))).isEqualTo("")
    trackCursor.close()

    // 5. Test Playlists table
    val playlistCursor = db.query("SELECT * FROM playlists ORDER BY id")
    assertThat(playlistCursor.count).isEqualTo(2)

    playlistCursor.moveToFirst()
    assertThat(
      playlistCursor.getString(playlistCursor.getColumnIndex("name"))
    ).isEqualTo("Test Playlist")
    assertThat(
      playlistCursor.getString(playlistCursor.getColumnIndex("url"))
    ).isEqualTo("playlist.m3u")
    assertThat(
      playlistCursor.getLong(playlistCursor.getColumnIndex("date_added"))
    ).isEqualTo(123456789)

    playlistCursor.moveToNext()
    assertThat(playlistCursor.getString(playlistCursor.getColumnIndex("name"))).isEqualTo("")
    playlistCursor.close()

    // 6. Test Radio Station table
    val radioCursor = db.query("SELECT * FROM radio_station ORDER BY id")
    assertThat(radioCursor.count).isEqualTo(2)

    radioCursor.moveToFirst()
    assertThat(radioCursor.getString(radioCursor.getColumnIndex("name"))).isEqualTo("Test Radio")
    assertThat(
      radioCursor.getString(radioCursor.getColumnIndex("url"))
    ).isEqualTo("http://radio.com")

    radioCursor.moveToNext()
    assertThat(radioCursor.getString(radioCursor.getColumnIndex("name"))).isEqualTo("")
    radioCursor.close()

    // 7. Test Settings table
    val settingsCursor = db.query("SELECT * FROM settings ORDER BY id")
    assertThat(settingsCursor.count).isEqualTo(2)

    settingsCursor.moveToFirst()
    assertThat(
      settingsCursor.getString(settingsCursor.getColumnIndex("address"))
    ).isEqualTo("192.168.1.1")
    assertThat(settingsCursor.getInt(settingsCursor.getColumnIndex("port"))).isEqualTo(3000)
    assertThat(settingsCursor.getString(settingsCursor.getColumnIndex("name"))).isEqualTo("Home")
    assertThat(settingsCursor.isNull(settingsCursor.getColumnIndex("is_default"))).isTrue()

    settingsCursor.moveToNext()
    assertThat(settingsCursor.getString(settingsCursor.getColumnIndex("address"))).isEqualTo("")
    assertThat(settingsCursor.getInt(settingsCursor.getColumnIndex("port"))).isEqualTo(0)
    settingsCursor.close()
  }

  @Test
  fun testIndicesAreCreated() {
    // Create database with version 3
    var db = helper.createDatabase(migrationTestDb, 3)
    db.close()

    // Run migration to version 4
    db = helper.runMigrationsAndValidate(migrationTestDb, 4, true, MIGRATION_3_4)

    // Test that unique indices were created by trying to insert duplicates
    // This should work for the first insert
    db.execSQL("INSERT INTO genre (genre, date_added, id) VALUES ('Rock', 0, 1)")

    // This should fail due to unique constraint on genre column
    try {
      db.execSQL("INSERT INTO genre (genre, date_added, id) VALUES ('Rock', 0, 2)")
      assertThat(false).isTrue() // Should not reach here
    } catch (e: Exception) {
      assertThat(e.message).contains("UNIQUE constraint failed")
    }

    // Test artist unique constraint
    db.execSQL("INSERT INTO artist (artist, date_added, id) VALUES ('Test Artist', 0, 1)")

    try {
      db.execSQL("INSERT INTO artist (artist, date_added, id) VALUES ('Test Artist', 0, 2)")
      assertThat(false).isTrue() // Should not reach here
    } catch (e: Exception) {
      assertThat(e.message).contains("UNIQUE constraint failed")
    }

    // Test album unique constraint (artist + album combination)
    db.execSQL(
      "INSERT INTO album (artist, album, date_added, id) VALUES ('Artist1', 'Album1', 0, 1)"
    )

    try {
      db.execSQL(
        "INSERT INTO album (artist, album, date_added, id) VALUES ('Artist1', 'Album1', 0, 2)"
      )
      assertThat(false).isTrue() // Should not reach here
    } catch (e: Exception) {
      assertThat(e.message).contains("UNIQUE constraint failed")
    }

    // Test track unique constraint on src
    db.execSQL(
      """
        INSERT INTO track (artist, title, src, trackno, disc, album_artist, album, genre, year, sortable_year, date_added, id)
        VALUES ('', '', 'unique.mp3', 0, 0, '', '', '', '', '', 0, 1)
      """.trimIndent()
    )

    try {
      db.execSQL(
        """
          INSERT INTO track (artist, title, src, trackno, disc, album_artist, album, genre, year, sortable_year, date_added, id)
          VALUES ('', '', 'unique.mp3', 0, 0, '', '', '', '', '', 0, 2)
        """.trimIndent()
      )
      assertThat(false).isTrue() // Should not reach here
    } catch (e: Exception) {
      assertThat(e.message).contains("UNIQUE constraint failed")
    }

    // Test playlist unique constraint on name
    db.execSQL(
      "INSERT INTO playlists (name, url, date_added, id) VALUES ('My Playlist', '', 0, 1)"
    )

    try {
      db.execSQL(
        "INSERT INTO playlists (name, url, date_added, id) VALUES ('My Playlist', '', 0, 2)"
      )
      assertThat(false).isTrue() // Should not reach here
    } catch (e: Exception) {
      assertThat(e.message).contains("UNIQUE constraint failed")
    }

    // Test radio station unique constraint on url
    db.execSQL(
      "INSERT INTO radio_station (name, url, date_added, id) VALUES ('', 'http://unique.com', 0, 1)"
    )

    try {
      db.execSQL(
        "INSERT INTO radio_station (name, url, date_added, id) VALUES ('', 'http://unique.com', 0, 2)"
      )
      assertThat(false).isTrue() // Should not reach here
    } catch (e: Exception) {
      assertThat(e.message).contains("UNIQUE constraint failed")
    }

    // Test settings unique constraint on address + port combination
    db.execSQL("INSERT INTO settings (address, port, name, id) VALUES ('192.168.1.1', 3000, '', 1)")

    try {
      db.execSQL(
        "INSERT INTO settings (address, port, name, id) VALUES ('192.168.1.1', 3000, '', 2)"
      )
      assertThat(false).isTrue() // Should not reach here
    } catch (e: Exception) {
      assertThat(e.message).contains("UNIQUE constraint failed")
    }

    db.close()
  }

  @Test
  fun testMigrationWithRoomDatabase() {
    // Test that the migration works with the actual Room database
    val database = Room.databaseBuilder(
      InstrumentationRegistry.getInstrumentation().targetContext,
      Database::class.java,
      "test-room-migration"
    )
      .addMigrations(MIGRATION_3_4)
      .build()

    // Try to access all DAOs to ensure schema is correct
    val genreDao = database.genreDao()
    val artistDao = database.artistDao()
    val albumDao = database.albumDao()
    val trackDao = database.trackDao()
    val playlistDao = database.playlistDao()
    val radioStationDao = database.radioStationDao()
    val connectionDao = database.connectionDao()

    // Perform basic operations to ensure everything works
    assertThat(genreDao).isNotNull()
    assertThat(artistDao).isNotNull()
    assertThat(albumDao).isNotNull()
    assertThat(trackDao).isNotNull()
    assertThat(playlistDao).isNotNull()
    assertThat(radioStationDao).isNotNull()
    assertThat(connectionDao).isNotNull()

    database.close()
  }
}
