@file:OptIn(ExperimentalCoroutinesApi::class)

package com.kelsos.mbrc.features.library.artists

import androidx.paging.testing.asSnapshot
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import com.kelsos.mbrc.common.data.Progress
import com.kelsos.mbrc.data.Database
import com.kelsos.mbrc.features.library.genres.GenreEntity
import com.kelsos.mbrc.features.library.tracks.TrackEntity
import com.kelsos.mbrc.networking.ApiBase
import com.kelsos.mbrc.networking.protocol.Protocol
import com.kelsos.mbrc.utils.testDispatcher
import com.kelsos.mbrc.utils.testDispatchers
import io.mockk.coEvery
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.core.module.dsl.bind
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.module
import org.koin.test.KoinTest
import org.koin.test.get
import org.koin.test.inject

@RunWith(AndroidJUnit4::class)
class ArtistRepositoryTest : KoinTest {
  private lateinit var database: Database
  private lateinit var dao: ArtistDao
  private val api: ApiBase = mockk()

  private val testModule =
    module {
      single { api }
      single { testDispatchers }
      single {
        Room
          .inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            Database::class.java,
          ).allowMainThreadQueries()
          .build()
      }
      single { get<Database>().artistDao() }
      singleOf(::ArtistRepositoryImpl) {
        bind<ArtistRepository>()
      }
    }

  private val repository: ArtistRepository by inject()

  @Before
  fun setUp() {
    startKoin { modules(listOf(testModule)) }
    database = get()
    dao = get()
  }

  @After
  fun tearDown() {
    database.close()
    stopKoin()
  }

  @Test
  fun countShouldReturnCorrectCount() {
    runTest(testDispatcher) {
      val artists =
        listOf(
          ArtistEntity(artist = "Artist 1", dateAdded = 1000L),
          ArtistEntity(artist = "Artist 2", dateAdded = 1000L),
          ArtistEntity(artist = "Artist 3", dateAdded = 1000L),
        )
      dao.insertAll(artists)

      val count = repository.count()

      assertThat(count).isEqualTo(3)
    }
  }

  @Test
  fun countShouldReturnZeroWhenEmpty() {
    runTest(testDispatcher) {
      val count = repository.count()

      assertThat(count).isEqualTo(0)
    }
  }

  @Test
  fun getArtistByGenreShouldReturnArtistsForGenre() {
    runTest(testDispatcher) {
      // Given: Artists and tracks with different genres
      val rockGenre = GenreEntity(genre = "Rock", dateAdded = 1000L)
      val popGenre = GenreEntity(genre = "Pop", dateAdded = 1000L)
      database.genreDao().insertAll(listOf(rockGenre, popGenre))

      // Get the inserted genre IDs
      val rockGenreId =
        database
          .genreDao()
          .genres()
          .first { it.genre == "Rock" }
          .id
      val popGenreId =
        database
          .genreDao()
          .genres()
          .first { it.genre == "Pop" }
          .id

      val artists =
        listOf(
          ArtistEntity(artist = "Rock Artist 1", dateAdded = 1000L),
          ArtistEntity(artist = "Rock Artist 2", dateAdded = 1000L),
          ArtistEntity(artist = "Pop Artist", dateAdded = 1000L),
        )
      dao.insertAll(artists)

      val tracks =
        listOf(
          TrackEntity(
            artist = "Rock Artist 1",
            title = "Rock Track 1",
            album = "Rock Album 1",
            genre = "Rock",
            dateAdded = 1000L,
          ),
          TrackEntity(
            artist = "Rock Artist 2",
            title = "Rock Track 2",
            album = "Rock Album 2",
            genre = "Rock",
            dateAdded = 1000L,
          ),
          TrackEntity(
            artist = "Pop Artist",
            title = "Pop Track",
            album = "Pop Album",
            genre = "Pop",
            dateAdded = 1000L,
          ),
        )
      database.trackDao().insertAll(tracks)

      // When: Get artists by rock genre
      val rockArtists = repository.getArtistByGenre(rockGenreId).asSnapshot()

      // Then: Should only include rock artists
      val artistNames = rockArtists.map { it.artist }
      assertThat(artistNames).containsExactly("Rock Artist 1", "Rock Artist 2")

      // When: Get artists by pop genre
      val popArtists = repository.getArtistByGenre(popGenreId).asSnapshot()

      // Then: Should only include pop artists
      val popArtistNames = popArtists.map { it.artist }
      assertThat(popArtistNames).containsExactly("Pop Artist")
    }
  }

  @Test
  fun searchShouldReturnMatchingArtists() {
    runTest(testDispatcher) {
      val artists =
        listOf(
          ArtistEntity(artist = "The Beatles", dateAdded = 1000L),
          ArtistEntity(artist = "The Beach Boys", dateAdded = 1000L),
          ArtistEntity(artist = "Queen", dateAdded = 1000L),
        )
      dao.insertAll(artists)

      val result = repository.search("The").asSnapshot()

      assertThat(result.map { it.artist }).containsExactly("The Beach Boys", "The Beatles")
    }
  }

  @Test
  fun searchShouldReturnEmptyWhenNoMatches() {
    runTest(testDispatcher) {
      val artists =
        listOf(
          ArtistEntity(artist = "The Beatles", dateAdded = 1000L),
          ArtistEntity(artist = "Queen", dateAdded = 1000L),
        )
      dao.insertAll(artists)

      val result = repository.search("Metallica").asSnapshot()

      assertThat(result).isEmpty()
    }
  }

  @Test
  fun getByIdShouldReturnArtistWhenExists() {
    runTest(testDispatcher) {
      val artist = ArtistEntity(artist = "The Beatles", dateAdded = 1000L)
      dao.insertAll(listOf(artist))
      val insertedArtist = dao.all().first()

      val result = repository.getById(insertedArtist.id!!)

      assertThat(result).isNotNull()
      assertThat(result!!.artist).isEqualTo("The Beatles")
      assertThat(result.id).isEqualTo(insertedArtist.id)
    }
  }

  @Test
  fun getByIdShouldReturnNullWhenNotExists() {
    runTest(testDispatcher) {
      val result = repository.getById(999L)

      assertThat(result).isNull()
    }
  }

  @Test
  fun getRemoteShouldFetchAndStoreNewArtists() {
    runTest(testDispatcher) {
      val remoteArtists =
        listOf(
          ArtistDto(artist = "Remote Artist 1"),
          ArtistDto(artist = "Remote Artist 2"),
        )
      coEvery {
        api.getAllPages(Protocol.LibraryBrowseArtists, ArtistDto::class, any())
      } returns flowOf(remoteArtists)

      repository.getRemote(null)

      val storedArtists = dao.all()
      assertThat(storedArtists.map { it.artist }).containsExactly("Remote Artist 1", "Remote Artist 2")
    }
  }

  @Test
  fun getRemoteShouldUpdateExistingArtists() {
    runTest(testDispatcher) {
      val existingArtist = ArtistEntity(artist = "Existing Artist", dateAdded = 500L)
      dao.insertAll(listOf(existingArtist))

      val remoteArtists = listOf(ArtistDto(artist = "Existing Artist"))
      coEvery {
        api.getAllPages(Protocol.LibraryBrowseArtists, ArtistDto::class, any())
      } returns flowOf(remoteArtists)

      repository.getRemote(null)

      val updatedArtists = dao.all()
      assertThat(updatedArtists).hasSize(1)
      assertThat(updatedArtists.first().artist).isEqualTo("Existing Artist")
      assertThat(updatedArtists.first().dateAdded).isGreaterThan(500L)
    }
  }

  @Test
  fun getRemoteShouldRemovePreviousEntries() {
    runTest(testDispatcher) {
      val oldArtist = ArtistEntity(artist = "Old Artist", dateAdded = 500L)
      dao.insertAll(listOf(oldArtist))

      val remoteArtists = listOf(ArtistDto(artist = "New Artist"))
      coEvery {
        api.getAllPages(Protocol.LibraryBrowseArtists, ArtistDto::class, any())
      } returns flowOf(remoteArtists)

      repository.getRemote(null)

      val storedArtists = dao.all()
      assertThat(storedArtists).hasSize(1)
      assertThat(storedArtists.first().artist).isEqualTo("New Artist")
    }
  }

  @Test
  fun getRemoteShouldHandleProgressCallback() {
    runTest(testDispatcher) {
      val progress: Progress = mockk(relaxed = true)
      val remoteArtists = listOf(ArtistDto(artist = "Artist"))
      coEvery {
        api.getAllPages(Protocol.LibraryBrowseArtists, ArtistDto::class, progress)
      } returns flowOf(remoteArtists)

      repository.getRemote(progress)

      @Suppress("IgnoredReturnValue")
      verify { api.getAllPages(Protocol.LibraryBrowseArtists, ArtistDto::class, progress) }
    }
  }

  @Test
  fun getAlbumArtistsOnlyShouldIncludeCompilationAlbumArtists() {
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
  fun getAlbumArtistsOnlyShouldExcludeTrackOnlyArtists() {
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
  fun getAllArtistsShouldSortIgnoringThePrefix() {
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
  fun getAlbumArtistsOnlyShouldSortIgnoringThePrefix() {
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
