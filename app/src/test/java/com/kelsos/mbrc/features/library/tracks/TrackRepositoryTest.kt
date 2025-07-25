@file:OptIn(ExperimentalCoroutinesApi::class)

package com.kelsos.mbrc.features.library.tracks

import androidx.paging.testing.asSnapshot
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import com.kelsos.mbrc.common.data.Progress
import com.kelsos.mbrc.data.Database
import com.kelsos.mbrc.networking.ApiBase
import com.kelsos.mbrc.networking.protocol.Protocol
import com.kelsos.mbrc.utils.TrackGenerator
import com.kelsos.mbrc.utils.testDispatcher
import com.kelsos.mbrc.utils.testDispatcherModule
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
import org.koin.test.inject

@RunWith(AndroidJUnit4::class)
class TrackRepositoryTest : KoinTest {
  // Constants for test data
  private companion object {
    const val OLDER_DATE_ADDED = TrackGenerator.OLDER_DATE_ADDED
  }

  private val testModule =
    module {
      single<ApiBase> { mockk() }
      single {
        Room
          .inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            Database::class.java
          ).allowMainThreadQueries()
          .build()
      }
      single { get<Database>().trackDao() }
      singleOf(::TrackRepositoryImpl) {
        bind<TrackRepository>()
      }
    }

  private val database: Database by inject()
  private val dao: TrackDao by inject()
  private val api: ApiBase by inject()

  private val repository: TrackRepository by inject()

  @Before
  fun setUp() {
    startKoin { modules(listOf(testModule, testDispatcherModule)) }
  }

  @After
  fun tearDown() {
    database.close()
    stopKoin()
  }

  // Helper methods for creating test data

  private fun createTrackDto(
    artist: String,
    title: String,
    src: String,
    trackno: Int = 1,
    disc: Int = 1,
    albumArtist: String = artist,
    album: String = "Album $artist",
    genre: String = "Rock",
    year: String = "2021"
  ): TrackDto = TrackDto(
    artist = artist,
    title = title,
    src = src,
    trackno = trackno,
    disc = disc,
    albumArtist = albumArtist,
    album = album,
    genre = genre,
    year = year
  )

  @Test
  fun countShouldReturnCorrectCount() {
    runTest(testDispatcher) {
      val tracks = TrackGenerator().generateTracks(3) { index, builder ->
        if (index == 2) builder.genre = "Pop"
        if (index == 3) builder.genre = "Jazz"
      }
      dao.insertAll(tracks)

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
  fun getAllShouldReturnAllTracksSorted() {
    runTest(testDispatcher) {
      val tracks = TrackGenerator().generateTracks(3) { index, builder ->
        if (index == 2) builder.genre = "Pop"
        if (index == 3) builder.genre = "Jazz"
      }
      dao.insertAll(tracks)

      val result = repository.getAll().asSnapshot()

      assertThat(result).hasSize(3)
      assertThat(result.map { it.title }).containsExactly("Track 1", "Track 2", "Track 3").inOrder()
    }
  }

  @Test
  fun getAllShouldReturnEmptyWhenNoTracks() {
    runTest(testDispatcher) {
      val result = repository.getAll().asSnapshot()

      assertThat(result).isEmpty()
    }
  }

  @Test
  fun getTracksShouldReturnAlbumTracks() {
    runTest(testDispatcher) {
      val album1Tracks = TrackGenerator(
        baseArtist = "Artist 1",
        baseTitle = "Album 1 Track",
        basePath = "/path/to/album1",
        baseAlbum = "Album 1"
      ).generateTracks(2)

      val album2Track = TrackGenerator(
        baseArtist = "Artist 2",
        baseTitle = "Album 2 Track",
        basePath = "/path/to/album2",
        baseAlbum = "Album 2",
        genre = "Pop"
      ).generateTrack()
      dao.insertAll(album1Tracks + album2Track)

      val query = PagingTrackQuery.Album(album = "Album 1", artist = "Artist 1")
      val result = repository.getTracks(query).asSnapshot()

      assertThat(result).hasSize(2)
      assertThat(
        result.map {
          it.title
        }
      ).containsExactly("Album 1 Track 1", "Album 1 Track 2").inOrder()
    }
  }

  @Test
  fun getTracksShouldReturnNonAlbumTracks() {
    runTest(testDispatcher) {
      val nonAlbumTracks = TrackGenerator(
        baseArtist = "Artist 1",
        baseTitle = "Non-Album Track",
        basePath = "/path/to/non-album",
        baseAlbum = ""
      ).generateTracks(2)

      val albumTrack = TrackGenerator(
        baseArtist = "Artist 1",
        baseTitle = "Album Track",
        basePath = "/path/to/album",
        baseAlbum = "Album"
      ).generateTrack()

      dao.insertAll(nonAlbumTracks + albumTrack)

      val query = PagingTrackQuery.NonAlbum(artist = "Artist 1")
      val result = repository.getTracks(query).asSnapshot()

      assertThat(result).hasSize(2)
      assertThat(
        result.map {
          it.title
        }
      ).containsExactly("Non-Album Track 1", "Non-Album Track 2").inOrder()
    }
  }

  @Test
  fun searchShouldReturnMatchingTracksByTitle() {
    runTest(testDispatcher) {
      val tracks = listOf(
        TrackGenerator(
          baseArtist = "Artist 1",
          baseTitle = "Rock Track",
          basePath = "/path/to"
        ).generateTrack { src = "/path/to/rock_track.mp3" },
        TrackGenerator(
          baseArtist = "Artist 2",
          baseTitle = "Pop Track",
          basePath = "/path/to",
          genre = "Pop"
        ).generateTrack { src = "/path/to/pop_track.mp3" },
        TrackGenerator(
          baseArtist = "Artist 3",
          baseTitle = "Another Rock Track",
          basePath = "/path/to"
        ).generateTrack { src = "/path/to/another_rock_track.mp3" }
      )
      dao.insertAll(tracks)

      val result = repository.search("Rock").asSnapshot()

      assertThat(result).hasSize(2)
      assertThat(result.map { it.title }).containsExactly("Rock Track", "Another Rock Track")
    }
  }

  @Test
  fun searchShouldReturnMatchingTracksByArtist() {
    runTest(testDispatcher) {
      val tracks = listOf(
        TrackGenerator(
          baseArtist = "Rock Artist",
          baseTitle = "Track 1",
          basePath = "/path/to"
        ).generateTrack { src = "/path/to/track1.mp3" },
        TrackGenerator(
          baseArtist = "Pop Artist",
          baseTitle = "Track 2",
          basePath = "/path/to",
          genre = "Pop"
        ).generateTrack { src = "/path/to/track2.mp3" },
        TrackGenerator(
          baseArtist = "Another Rock Artist",
          baseTitle = "Track 3",
          basePath = "/path/to"
        ).generateTrack { src = "/path/to/track3.mp3" }
      )
      dao.insertAll(tracks)

      val result = repository.search("Rock").asSnapshot()

      assertThat(result).hasSize(2)
      assertThat(result.map { it.artist }).containsExactly("Rock Artist", "Another Rock Artist")
    }
  }

  @Test
  fun searchShouldReturnMatchingTracksByTitleOrArtist() {
    runTest(testDispatcher) {
      val tracks = listOf(
        TrackGenerator(
          baseArtist = "Jazz Artist",
          baseTitle = "Rock Track", // Title matches
          basePath = "/path/to"
        ).generateTrack { src = "/path/to/track1.mp3" },
        TrackGenerator(
          baseArtist = "Rock Artist", // Artist matches
          baseTitle = "Pop Track",
          basePath = "/path/to"
        ).generateTrack { src = "/path/to/track2.mp3" },
        TrackGenerator(
          baseArtist = "Pop Artist",
          baseTitle = "Blues Track", // No match
          basePath = "/path/to"
        ).generateTrack { src = "/path/to/track3.mp3" },
        TrackGenerator(
          baseArtist = "Rock Star", // Artist matches
          baseTitle = "Jazz Track",
          basePath = "/path/to"
        ).generateTrack { src = "/path/to/track4.mp3" }
      )
      dao.insertAll(tracks)

      val result = repository.search("Rock").asSnapshot()

      assertThat(result).hasSize(3)
      assertThat(result.map { it.src }).containsExactly(
        "/path/to/track1.mp3",
        "/path/to/track2.mp3",
        "/path/to/track4.mp3"
      )
    }
  }

  @Test
  fun searchShouldReturnEmptyWhenNoMatches() {
    runTest(testDispatcher) {
      val tracks = TrackGenerator().generateTracks(2) { index, builder ->
        builder.title = if (index == 1) "Rock Track" else "Pop Track"
        builder.src = "/path/to/${builder.title.lowercase().replace(" ", "_")}.mp3"
        if (index == 2) builder.genre = "Pop"
      }
      dao.insertAll(tracks)

      val result = repository.search("Jazz").asSnapshot()

      assertThat(result).isEmpty()
    }
  }

  @Test
  fun searchShouldBeCaseInsensitive() {
    runTest(testDispatcher) {
      val tracks = listOf(
        TrackGenerator(
          baseArtist = "ROCK ARTIST",
          baseTitle = "Classical Music",
          basePath = "/path/to"
        ).generateTrack { src = "/path/to/track1.mp3" },
        TrackGenerator(
          baseArtist = "Jazz Musician",
          baseTitle = "ROCK ANTHEM",
          basePath = "/path/to"
        ).generateTrack { src = "/path/to/track2.mp3" },
        TrackGenerator(
          baseArtist = "Pop Star",
          baseTitle = "Dancing Queen",
          basePath = "/path/to"
        ).generateTrack { src = "/path/to/track3.mp3" }
      )
      dao.insertAll(tracks)

      val result = repository.search("rock").asSnapshot()

      assertThat(result).hasSize(2)
      assertThat(
        result.map {
          it.src
        }
      ).containsExactly("/path/to/track1.mp3", "/path/to/track2.mp3")
    }
  }

  @Test
  fun getTrackPathsShouldReturnAllTrackPaths() {
    runTest(testDispatcher) {
      val tracks = TrackGenerator().generateTracks(2) { index, builder ->
        if (index == 2) builder.genre = "Pop"
      }
      dao.insertAll(tracks)

      val result = repository.getTrackPaths(TrackQuery.All)

      assertThat(result).hasSize(2)
      assertThat(result).containsExactly("/path/to/track1.mp3", "/path/to/track2.mp3")
    }
  }

  @Test
  fun getTrackPathsShouldReturnGenreTrackPaths() {
    runTest(testDispatcher) {
      val rockTracks = TrackGenerator(
        baseTitle = "Rock Track",
        basePath = "/path/to",
        genre = "Rock"
      ).generateTracks(2) { index, builder ->
        builder.artist = "Artist $index"
        builder.src = "/path/to/rock_track$index.mp3"
      }

      val popTrack = TrackGenerator(
        baseArtist = "Artist 3",
        baseTitle = "Pop Track",
        basePath = "/path/to",
        genre = "Pop"
      ).generateTrack { src = "/path/to/pop_track.mp3" }

      dao.insertAll(rockTracks + popTrack)

      val result = repository.getTrackPaths(TrackQuery.Genre(genre = "Rock"))

      assertThat(result).hasSize(2)
      assertThat(result).containsExactly("/path/to/rock_track1.mp3", "/path/to/rock_track2.mp3")
    }
  }

  @Test
  fun getTrackPathsShouldReturnArtistTrackPaths() {
    runTest(testDispatcher) {
      val artist1Tracks = TrackGenerator(
        baseArtist = "Artist 1",
        baseTitle = "Track",
        baseAlbum = "Album 1",
        basePath = "/path/to"
      ).generateTracks(2) { index, builder ->
        builder.src = "/path/to/artist1_track$index.mp3"
      }

      val artist2Track = TrackGenerator(
        baseArtist = "Artist 2",
        baseTitle = "Track 1",
        baseAlbum = "Album 2",
        basePath = "/path/to",
        genre = "Pop"
      ).generateTrack { src = "/path/to/artist2_track1.mp3" }

      dao.insertAll(artist1Tracks + artist2Track)

      val result = repository.getTrackPaths(TrackQuery.Artist(artist = "Artist 1"))

      assertThat(result).hasSize(2)
      assertThat(
        result
      ).containsExactly("/path/to/artist1_track1.mp3", "/path/to/artist1_track2.mp3")
    }
  }

  @Test
  fun getTrackPathsShouldReturnAlbumTrackPaths() {
    runTest(testDispatcher) {
      val album1Tracks = TrackGenerator(
        baseArtist = "Artist 1",
        baseTitle = "Album 1 Track",
        baseAlbum = "Album 1",
        basePath = "/path/to"
      ).generateTracks(2) { index, builder ->
        builder.src = "/path/to/album1_track$index.mp3"
      }

      val album2Track = TrackGenerator(
        baseArtist = "Artist 1",
        baseTitle = "Album 2 Track 1",
        baseAlbum = "Album 2",
        basePath = "/path/to"
      ).generateTrack { src = "/path/to/album2_track1.mp3" }

      dao.insertAll(album1Tracks + album2Track)

      val result = repository.getTrackPaths(
        TrackQuery.Album(album = "Album 1", artist = "Artist 1")
      )

      assertThat(result).hasSize(2)
      assertThat(result).containsExactly("/path/to/album1_track1.mp3", "/path/to/album1_track2.mp3")
    }
  }

  @Test
  fun getByIdShouldReturnTrackWhenExists() {
    runTest(testDispatcher) {
      val track = TrackGenerator(
        baseArtist = "Artist 1",
        baseTitle = "Track 1",
        basePath = "/path/to"
      ).generateTrack { src = "/path/to/track1.mp3" }

      dao.insertAll(listOf(track))
      val insertedTrack = dao.all().first()

      val result = repository.getById(insertedTrack.id)

      assertThat(result).isNotNull()
      assertThat(result!!.title).isEqualTo("Track 1")
      assertThat(result.artist).isEqualTo("Artist 1")
      assertThat(result.id).isEqualTo(insertedTrack.id)
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
  fun getRemoteShouldFetchAndStoreNewTracks() {
    runTest(testDispatcher) {
      val remoteTracks =
        listOf(
          createTrackDto(
            artist = "Artist 1",
            title = "Track 1",
            src = "/path/to/track1.mp3",
            year = "2021"
          ),
          createTrackDto(
            artist = "Artist 2",
            title = "Track 2",
            src = "/path/to/track2.mp3",
            genre = "Pop",
            year = "2022"
          )
        )
      coEvery {
        api.getAllPages(Protocol.LibraryBrowseTracks, TrackDto::class, any())
      } returns flowOf(remoteTracks)

      repository.getRemote(null)

      val storedTracks = dao.all()
      assertThat(storedTracks).hasSize(2)
      assertThat(storedTracks.map { it.title }).containsExactly("Track 1", "Track 2")
    }
  }

  @Test
  fun getRemoteShouldUpdateExistingTracks() {
    runTest(testDispatcher) {
      val existingTrack = TrackGenerator(
        baseArtist = "Artist 1",
        baseTitle = "Track 1 Old",
        baseAlbum = "Album 1 Old",
        basePath = "/path/to",
        dateAdded = OLDER_DATE_ADDED
      ).generateTrack { src = "/path/to/track1.mp3" }

      dao.insertAll(listOf(existingTrack))
      val insertedId = dao.all().first { it.src == "/path/to/track1.mp3" }.id

      val remoteTracks =
        listOf(
          createTrackDto(
            artist = "Artist 1",
            title = "Track 1 New",
            src = "/path/to/track1.mp3",
            album = "Album 1 New",
            year = "2021"
          )
        )
      coEvery {
        api.getAllPages(Protocol.LibraryBrowseTracks, TrackDto::class, any())
      } returns flowOf(remoteTracks)

      repository.getRemote(null)

      val updatedTracks = dao.all()
      assertThat(updatedTracks).hasSize(1)
      assertThat(updatedTracks.first().id).isEqualTo(insertedId)
      assertThat(updatedTracks.first().title).isEqualTo("Track 1 New")
      assertThat(updatedTracks.first().album).isEqualTo("Album 1 New")
      assertThat(updatedTracks.first().dateAdded).isGreaterThan(OLDER_DATE_ADDED)
    }
  }

  @Test
  fun getRemoteShouldRemovePreviousEntries() {
    runTest(testDispatcher) {
      val oldTrack = TrackGenerator(
        baseArtist = "Old Artist",
        baseTitle = "Old Track",
        baseAlbum = "Old Album",
        basePath = "/path/to",
        genre = "Old Genre",
        dateAdded = OLDER_DATE_ADDED
      ).generateTrack { src = "/path/to/old_track.mp3" }

      dao.insertAll(listOf(oldTrack))

      val remoteTracks =
        listOf(
          createTrackDto(
            artist = "New Artist",
            title = "New Track",
            src = "/path/to/new_track.mp3",
            album = "New Album",
            genre = "New Genre",
            year = "2023"
          )
        )
      coEvery {
        api.getAllPages(Protocol.LibraryBrowseTracks, TrackDto::class, any())
      } returns flowOf(remoteTracks)

      repository.getRemote(null)

      val storedTracks = dao.all()
      assertThat(storedTracks).hasSize(1)
      assertThat(storedTracks.first().title).isEqualTo("New Track")
      assertThat(storedTracks.first().src).isEqualTo("/path/to/new_track.mp3")
    }
  }

  @Test
  fun getRemoteShouldHandleProgressCallback() {
    runTest(testDispatcher) {
      val progress: Progress = mockk(relaxed = true)
      val remoteTracks =
        listOf(
          createTrackDto(
            artist = "Artist 1",
            title = "Track 1",
            src = "/path/to/track1.mp3",
            year = "2021"
          )
        )
      coEvery {
        api.getAllPages(Protocol.LibraryBrowseTracks, TrackDto::class, progress)
      } returns flowOf(remoteTracks)

      repository.getRemote(progress)

      @Suppress("IgnoredReturnValue")
      verify { api.getAllPages(Protocol.LibraryBrowseTracks, TrackDto::class, progress) }
    }
  }

  @Test
  fun getRemoteShouldHandleMixOfNewAndExistingTracks() {
    runTest(testDispatcher) {
      val existingTracks = listOf(
        TrackGenerator(
          baseArtist = "Artist 1",
          baseTitle = "Track 1",
          basePath = "/path/to",
          dateAdded = OLDER_DATE_ADDED
        ).generateTrack { src = "/path/to/track1.mp3" },
        TrackGenerator(
          baseArtist = "Artist 2",
          baseTitle = "Track 2",
          basePath = "/path/to",
          genre = "Jazz",
          dateAdded = OLDER_DATE_ADDED
        ).generateTrack { src = "/path/to/track2.mp3" }
      )
      dao.insertAll(existingTracks)
      val track1Id = dao.all().first { it.src == "/path/to/track1.mp3" }.id

      val remoteTracks =
        listOf(
          createTrackDto(
            artist = "Artist 1",
            title = "Track 1 Updated",
            src = "/path/to/track1.mp3",
            year = "2021"
          ),
          createTrackDto(
            artist = "Artist 3",
            title = "Track 3",
            src = "/path/to/track3.mp3",
            album = "Album 3",
            genre = "Pop",
            year = "2023"
          )
        )
      coEvery {
        api.getAllPages(Protocol.LibraryBrowseTracks, TrackDto::class, any())
      } returns flowOf(remoteTracks)

      repository.getRemote(null)

      val storedTracks = dao.all().sortedBy { it.title }
      assertThat(storedTracks).hasSize(2)
      assertThat(storedTracks.map { it.title }).containsExactly("Track 1 Updated", "Track 3")
      assertThat(storedTracks.first { it.src == "/path/to/track1.mp3" }.id).isEqualTo(track1Id)
    }
  }
}
