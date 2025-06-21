@file:OptIn(ExperimentalCoroutinesApi::class)

package com.kelsos.mbrc.features.library.artists

import android.os.Build
import androidx.paging.testing.asSnapshot
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import com.kelsos.mbrc.TestApplication
import com.kelsos.mbrc.data.Database
import com.kelsos.mbrc.features.library.tracks.TrackEntity
import com.kelsos.mbrc.networking.ApiBase
import com.kelsos.mbrc.utils.testDispatchers
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(
  application = TestApplication::class,
  sdk = [Build.VERSION_CODES.VANILLA_ICE_CREAM],
)
class ArtistRepositoryTest {
  private lateinit var database: Database
  private lateinit var repository: ArtistRepository
  private lateinit var dao: ArtistDao
  private val api: ApiBase = mockk()

  @Before
  fun setUp() {
    database =
      Room
        .inMemoryDatabaseBuilder(
          ApplicationProvider.getApplicationContext(),
          Database::class.java,
        ).allowMainThreadQueries()
        .build()
    dao = database.artistDao()
    repository = ArtistRepositoryImpl(dao, api, testDispatchers)
  }

  @After
  fun tearDown() {
    database.close()
  }

  @Test
  fun getAlbumArtistsOnly_shouldIncludeCompilationAlbumArtists() {
    runTest {
      // Given: Compilation album with different track artists and album artist
      val albumArtist = "Various Artists"
      val tracks =
        listOf(
          TrackEntity(
            artist = "Artist 1",
            title = "Track 1",
            album = "Compilation Album",
            albumArtist = albumArtist,
            src = "track1.mp3",
            dateAdded = 1000L,
          ),
          TrackEntity(
            artist = "Artist 2",
            title = "Track 2",
            album = "Compilation Album",
            albumArtist = albumArtist,
            src = "track2.mp3",
            dateAdded = 1000L,
          ),
        )

      val artists =
        listOf(
          ArtistEntity(artist = "Artist 1", dateAdded = 1000L),
          ArtistEntity(artist = "Artist 2", dateAdded = 1000L),
          ArtistEntity(artist = albumArtist, dateAdded = 1000L),
        )

      // Insert test data
      database.trackDao().insertAll(tracks)
      dao.insertAll(artists)

      // When: Get album artists only
      val albumArtists = repository.getAlbumArtistsOnly().asSnapshot()

      // Then: Should include the compilation album artist
      val artistNames = albumArtists.map { it.artist }
      assertThat(artistNames).containsExactly(albumArtist)
    }
  }

  @Test
  fun getAlbumArtistsOnly_shouldExcludeTrackOnlyArtists() {
    runTest {
      // Given: Regular album where track artist matches album artist,
      // and another track where artist doesn't match album artist
      val regularAlbumArtist = "Regular Artist"
      val tracks =
        listOf(
          TrackEntity(
            artist = regularAlbumArtist,
            title = "Track 1",
            album = "Regular Album",
            albumArtist = regularAlbumArtist,
            src = "track1.mp3",
            dateAdded = 1000L,
          ),
          TrackEntity(
            artist = "Different Artist", // This artist should not appear in album artists
            title = "Track 2",
            album = "Different Album",
            albumArtist = "Different Album Artist",
            src = "track2.mp3",
            dateAdded = 1000L,
          ),
        )

      val artists =
        listOf(
          ArtistEntity(artist = regularAlbumArtist, dateAdded = 1000L),
          ArtistEntity(artist = "Different Artist", dateAdded = 1000L),
          ArtistEntity(artist = "Different Album Artist", dateAdded = 1000L),
        )

      // Insert test data
      database.trackDao().insertAll(tracks)
      dao.insertAll(artists)

      // When: Get album artists only
      val albumArtists = repository.getAlbumArtistsOnly().asSnapshot()

      // Then: Should only include artists who are album artists
      val artistNames = albumArtists.map { it.artist }
      assertThat(artistNames).containsExactly(regularAlbumArtist, "Different Album Artist")
    }
  }

  @Test
  fun getAllArtists_shouldSortIgnoringThePrefix() {
    runTest {
      // Given: Artists with and without "the" prefix
      val artists =
        listOf(
          ArtistEntity(artist = "The Beatles", dateAdded = 1000L),
          ArtistEntity(artist = "Adele", dateAdded = 1000L),
          ArtistEntity(artist = "The Rolling Stones", dateAdded = 1000L),
          ArtistEntity(artist = "Bob Dylan", dateAdded = 1000L),
          ArtistEntity(artist = "The Who", dateAdded = 1000L),
        )

      // Insert test data
      dao.insertAll(artists)

      // When: Get all artists
      val allArtists = repository.getAll().asSnapshot()

      // Then: Should be sorted ignoring "the" prefix
      // Expected order: Adele, The Beatles, Bob Dylan, The Rolling Stones, The Who
      val artistNames = allArtists.map { it.artist }
      assertThat(artistNames)
        .containsExactly(
          "Adele",
          "The Beatles",
          "Bob Dylan",
          "The Rolling Stones",
          "The Who",
        ).inOrder()
    }
  }

  @Test
  fun getAlbumArtistsOnly_shouldSortIgnoringThePrefix() {
    runTest {
      // Given: Album artists with "the" prefix
      val tracks =
        listOf(
          TrackEntity(
            artist = "John Lennon",
            title = "Track 1",
            album = "Album 1",
            albumArtist = "The Beatles",
            src = "track1.mp3",
            dateAdded = 1000L,
          ),
          TrackEntity(
            artist = "Bob Dylan",
            title = "Track 2",
            album = "Album 2",
            albumArtist = "Bob Dylan",
            src = "track2.mp3",
            dateAdded = 1000L,
          ),
          TrackEntity(
            artist = "Roger Daltrey",
            title = "Track 3",
            album = "Album 3",
            albumArtist = "The Who",
            src = "track3.mp3",
            dateAdded = 1000L,
          ),
        )

      val artists =
        listOf(
          ArtistEntity(artist = "The Beatles", dateAdded = 1000L),
          ArtistEntity(artist = "Bob Dylan", dateAdded = 1000L),
          ArtistEntity(artist = "The Who", dateAdded = 1000L),
        )

      // Insert test data
      database.trackDao().insertAll(tracks)
      dao.insertAll(artists)

      // When: Get album artists only
      val albumArtists = repository.getAlbumArtistsOnly().asSnapshot()

      // Then: Should be sorted ignoring "the" prefix
      // Expected order: The Beatles, Bob Dylan, The Who
      val artistNames = albumArtists.map { it.artist }
      assertThat(artistNames)
        .containsExactly(
          "The Beatles",
          "Bob Dylan",
          "The Who",
        ).inOrder()
    }
  }
}
