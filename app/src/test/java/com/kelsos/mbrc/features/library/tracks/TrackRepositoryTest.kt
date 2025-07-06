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
    const val DEFAULT_DATE_ADDED = 1000L
    const val OLDER_DATE_ADDED = 500L
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
  private fun createTrackEntity(
    artist: String,
    title: String,
    src: String,
    trackno: Int = 1,
    disc: Int = 1,
    albumArtist: String = artist,
    album: String = "Album $artist",
    genre: String = "Rock",
    dateAdded: Long = DEFAULT_DATE_ADDED
  ): TrackEntity = TrackEntity(
    artist = artist,
    title = title,
    src = src,
    trackno = trackno,
    disc = disc,
    albumArtist = albumArtist,
    album = album,
    genre = genre,
    dateAdded = dateAdded
  )

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
      val tracks =
        listOf(
          createTrackEntity(
            artist = "Artist 1",
            title = "Track 1",
            src = "/path/to/track1.mp3"
          ),
          createTrackEntity(
            artist = "Artist 2",
            title = "Track 2",
            src = "/path/to/track2.mp3",
            trackno = 2,
            genre = "Pop"
          ),
          createTrackEntity(
            artist = "Artist 3",
            title = "Track 3",
            src = "/path/to/track3.mp3",
            trackno = 3,
            genre = "Jazz"
          )
        )
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
      val tracks =
        listOf(
          createTrackEntity(
            artist = "Artist 1",
            title = "Track 1",
            src = "/path/to/track1.mp3"
          ),
          createTrackEntity(
            artist = "Artist 2",
            title = "Track 2",
            src = "/path/to/track2.mp3",
            trackno = 2,
            genre = "Pop"
          ),
          createTrackEntity(
            artist = "Artist 3",
            title = "Track 3",
            src = "/path/to/track3.mp3",
            trackno = 3,
            genre = "Jazz"
          )
        )
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
      val tracks =
        listOf(
          createTrackEntity(
            artist = "Artist 1",
            title = "Album 1 Track 1",
            src = "/path/to/album1/track1.mp3",
            album = "Album 1"
          ),
          createTrackEntity(
            artist = "Artist 1",
            title = "Album 1 Track 2",
            src = "/path/to/album1/track2.mp3",
            trackno = 2,
            album = "Album 1"
          ),
          createTrackEntity(
            artist = "Artist 2",
            title = "Album 2 Track 1",
            src = "/path/to/album2/track1.mp3",
            album = "Album 2",
            genre = "Pop"
          )
        )
      dao.insertAll(tracks)

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
      val tracks =
        listOf(
          createTrackEntity(
            artist = "Artist 1",
            title = "Non-Album Track 1",
            src = "/path/to/non-album/track1.mp3",
            album = ""
          ),
          createTrackEntity(
            artist = "Artist 1",
            title = "Non-Album Track 2",
            src = "/path/to/non-album/track2.mp3",
            trackno = 2,
            album = ""
          ),
          createTrackEntity(
            artist = "Artist 1",
            title = "Album Track",
            src = "/path/to/album/track.mp3",
            album = "Album"
          )
        )
      dao.insertAll(tracks)

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
  fun searchShouldReturnMatchingTracks() {
    runTest(testDispatcher) {
      val tracks =
        listOf(
          createTrackEntity(
            artist = "Artist 1",
            title = "Rock Track",
            src = "/path/to/rock_track.mp3"
          ),
          createTrackEntity(
            artist = "Artist 2",
            title = "Pop Track",
            src = "/path/to/pop_track.mp3",
            genre = "Pop"
          ),
          createTrackEntity(
            artist = "Artist 3",
            title = "Another Rock Track",
            src = "/path/to/another_rock_track.mp3"
          )
        )
      dao.insertAll(tracks)

      val result = repository.search("Rock").asSnapshot()

      assertThat(result).hasSize(2)
      assertThat(result.map { it.title }).containsExactly("Rock Track", "Another Rock Track")
    }
  }

  @Test
  fun searchShouldReturnEmptyWhenNoMatches() {
    runTest(testDispatcher) {
      val tracks =
        listOf(
          createTrackEntity(
            artist = "Artist 1",
            title = "Rock Track",
            src = "/path/to/rock_track.mp3"
          ),
          createTrackEntity(
            artist = "Artist 2",
            title = "Pop Track",
            src = "/path/to/pop_track.mp3",
            genre = "Pop"
          )
        )
      dao.insertAll(tracks)

      val result = repository.search("Jazz").asSnapshot()

      assertThat(result).isEmpty()
    }
  }

  @Test
  fun getTrackPathsShouldReturnAllTrackPaths() {
    runTest(testDispatcher) {
      val tracks =
        listOf(
          createTrackEntity(
            artist = "Artist 1",
            title = "Track 1",
            src = "/path/to/track1.mp3"
          ),
          createTrackEntity(
            artist = "Artist 2",
            title = "Track 2",
            src = "/path/to/track2.mp3",
            genre = "Pop"
          )
        )
      dao.insertAll(tracks)

      val result = repository.getTrackPaths(TrackQuery.All)

      assertThat(result).hasSize(2)
      assertThat(result).containsExactly("/path/to/track1.mp3", "/path/to/track2.mp3")
    }
  }

  @Test
  fun getTrackPathsShouldReturnGenreTrackPaths() {
    runTest(testDispatcher) {
      val tracks =
        listOf(
          createTrackEntity(
            artist = "Artist 1",
            title = "Rock Track 1",
            src = "/path/to/rock_track1.mp3",
            genre = "Rock"
          ),
          createTrackEntity(
            artist = "Artist 2",
            title = "Rock Track 2",
            src = "/path/to/rock_track2.mp3",
            genre = "Rock"
          ),
          createTrackEntity(
            artist = "Artist 3",
            title = "Pop Track",
            src = "/path/to/pop_track.mp3",
            genre = "Pop"
          )
        )
      dao.insertAll(tracks)

      val result = repository.getTrackPaths(TrackQuery.Genre(genre = "Rock"))

      assertThat(result).hasSize(2)
      assertThat(result).containsExactly("/path/to/rock_track1.mp3", "/path/to/rock_track2.mp3")
    }
  }

  @Test
  fun getTrackPathsShouldReturnArtistTrackPaths() {
    runTest(testDispatcher) {
      val tracks =
        listOf(
          createTrackEntity(
            artist = "Artist 1",
            title = "Track 1",
            src = "/path/to/artist1_track1.mp3",
            album = "Album 1"
          ),
          createTrackEntity(
            artist = "Artist 1",
            title = "Track 2",
            src = "/path/to/artist1_track2.mp3",
            trackno = 2,
            album = "Album 1"
          ),
          createTrackEntity(
            artist = "Artist 2",
            title = "Track 1",
            src = "/path/to/artist2_track1.mp3",
            album = "Album 2",
            genre = "Pop"
          )
        )
      dao.insertAll(tracks)

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
      val tracks =
        listOf(
          createTrackEntity(
            artist = "Artist 1",
            title = "Album 1 Track 1",
            src = "/path/to/album1_track1.mp3",
            album = "Album 1"
          ),
          createTrackEntity(
            artist = "Artist 1",
            title = "Album 1 Track 2",
            src = "/path/to/album1_track2.mp3",
            trackno = 2,
            album = "Album 1"
          ),
          createTrackEntity(
            artist = "Artist 1",
            title = "Album 2 Track 1",
            src = "/path/to/album2_track1.mp3",
            album = "Album 2"
          )
        )
      dao.insertAll(tracks)

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
      val track =
        createTrackEntity(
          artist = "Artist 1",
          title = "Track 1",
          src = "/path/to/track1.mp3"
        )
      dao.insertAll(listOf(track))
      val insertedTrack = dao.all().first()

      val result = repository.getById(insertedTrack.id!!)

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
      val existingTrack =
        createTrackEntity(
          artist = "Artist 1",
          title = "Track 1 Old",
          src = "/path/to/track1.mp3",
          album = "Album 1 Old",
          dateAdded = OLDER_DATE_ADDED
        )
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
      val oldTrack =
        createTrackEntity(
          artist = "Old Artist",
          title = "Old Track",
          src = "/path/to/old_track.mp3",
          album = "Old Album",
          genre = "Old Genre",
          dateAdded = OLDER_DATE_ADDED
        )
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
      val existingTracks =
        listOf(
          createTrackEntity(
            artist = "Artist 1",
            title = "Track 1",
            src = "/path/to/track1.mp3",
            dateAdded = OLDER_DATE_ADDED
          ),
          createTrackEntity(
            artist = "Artist 2",
            title = "Track 2",
            src = "/path/to/track2.mp3",
            genre = "Jazz",
            dateAdded = OLDER_DATE_ADDED
          )
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
