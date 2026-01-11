@file:OptIn(ExperimentalCoroutinesApi::class)

package com.kelsos.mbrc.feature.library.albums

import androidx.paging.testing.asSnapshot
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import com.kelsos.mbrc.core.common.data.Progress
import com.kelsos.mbrc.core.common.settings.AlbumSortField
import com.kelsos.mbrc.core.common.settings.SortOrder
import com.kelsos.mbrc.core.common.test.testDispatcher
import com.kelsos.mbrc.core.common.test.testDispatcherModule
import com.kelsos.mbrc.core.data.Database
import com.kelsos.mbrc.core.data.library.album.AlbumCover
import com.kelsos.mbrc.core.data.library.album.AlbumDao
import com.kelsos.mbrc.core.data.library.album.AlbumEntity
import com.kelsos.mbrc.core.data.library.album.AlbumRepository
import com.kelsos.mbrc.core.data.library.track.TrackDao
import com.kelsos.mbrc.core.data.library.track.TrackEntity
import com.kelsos.mbrc.core.data.test.testDatabaseModule
import com.kelsos.mbrc.core.networking.api.LibraryApi
import com.kelsos.mbrc.core.networking.dto.AlbumDto
import io.mockk.every
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
class AlbumRepositoryTest : KoinTest {
  private val testModule =
    module {
      single<LibraryApi> { mockk() }
      singleOf(::AlbumRepositoryImpl) {
        bind<AlbumRepository>()
      }
    }

  private val database: Database by inject()
  private val dao: AlbumDao by inject()
  private val trackDao: TrackDao by inject()
  private val libraryApi: LibraryApi by inject()

  private val repository: AlbumRepository by inject()

  @Before
  fun setUp() {
    startKoin { modules(listOf(testModule, testDatabaseModule, testDispatcherModule)) }
  }

  @After
  fun tearDown() {
    database.close()
    stopKoin()
  }

  @Test
  fun countShouldReturnCorrectCount() {
    runTest(testDispatcher) {
      val albums =
        listOf(
          AlbumEntity(artist = "Artist1", album = "Album1", dateAdded = 1000L),
          AlbumEntity(artist = "Artist2", album = "Album2", dateAdded = 1000L),
          AlbumEntity(artist = "Artist3", album = "Album3", dateAdded = 1000L)
        )
      dao.insert(albums)

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
  fun getAllShouldReturnAllAlbumsSorted() {
    runTest(testDispatcher) {
      val albums =
        listOf(
          AlbumEntity(artist = "Artist1", album = "Album C", dateAdded = 1000L),
          AlbumEntity(artist = "Artist2", album = "Album A", dateAdded = 1000L),
          AlbumEntity(artist = "Artist3", album = "Album B", dateAdded = 1000L)
        )
      dao.insert(albums)

      val result = repository.getAll().asSnapshot()

      assertThat(result.map { it.album }).containsExactly("Album A", "Album B", "Album C").inOrder()
    }
  }

  @Test
  fun getAllShouldReturnEmptyWhenNoAlbums() {
    runTest(testDispatcher) {
      val result = repository.getAll().asSnapshot()

      assertThat(result).isEmpty()
    }
  }

  @Test
  fun searchShouldReturnMatchingAlbumsByAlbumName() {
    runTest(testDispatcher) {
      val albums =
        listOf(
          AlbumEntity(artist = "Artist1", album = "Rock Album", dateAdded = 1000L),
          AlbumEntity(artist = "Artist2", album = "Pop Rock", dateAdded = 1000L),
          AlbumEntity(artist = "Artist3", album = "Jazz", dateAdded = 1000L),
          AlbumEntity(artist = "Artist4", album = "Hard Rock", dateAdded = 1000L)
        )
      dao.insert(albums)

      val result = repository.search("Rock").asSnapshot()

      // The DAO sorts by album name, so the order is alphabetical
      assertThat(
        result.map {
          it.album
        }
      ).containsExactly("Hard Rock", "Pop Rock", "Rock Album").inOrder()
    }
  }

  @Test
  fun searchShouldReturnEmptyWhenNoMatches() {
    runTest(testDispatcher) {
      val albums =
        listOf(
          AlbumEntity(artist = "Artist1", album = "Rock Album", dateAdded = 1000L),
          AlbumEntity(artist = "Artist2", album = "Pop", dateAdded = 1000L)
        )
      dao.insert(albums)

      val result = repository.search("Classical").asSnapshot()

      assertThat(result).isEmpty()
    }
  }

  @Test
  fun searchShouldBeCaseInsensitive() {
    runTest(testDispatcher) {
      val albums =
        listOf(
          AlbumEntity(artist = "Artist1", album = "Rock Album", dateAdded = 1000L),
          AlbumEntity(artist = "Artist2", album = "JAZZ", dateAdded = 1000L),
          AlbumEntity(artist = "Artist3", album = "pop", dateAdded = 1000L)
        )
      dao.insert(albums)

      val result = repository.search("rock").asSnapshot()

      assertThat(result.map { it.album }).containsExactly("Rock Album")
    }
  }

  @Test
  fun searchShouldReturnMatchingAlbumsByArtistName() {
    runTest(testDispatcher) {
      val albums =
        listOf(
          AlbumEntity(artist = "Metallica", album = "Master of Puppets", dateAdded = 1000L),
          AlbumEntity(artist = "Metallica", album = "Black Album", dateAdded = 1000L),
          AlbumEntity(artist = "Iron Maiden", album = "The Number of the Beast", dateAdded = 1000L),
          AlbumEntity(artist = "Black Sabbath", album = "Paranoid", dateAdded = 1000L)
        )
      dao.insert(albums)

      val result = repository.search("Metallica").asSnapshot()

      // Should return albums by Metallica, sorted by album name
      assertThat(
        result.map {
          it.album
        }
      ).containsExactly("Black Album", "Master of Puppets").inOrder()
      assertThat(result.map { it.artist }).containsExactly("Metallica", "Metallica")
    }
  }

  @Test
  fun searchShouldReturnMatchingAlbumsByPartialArtistName() {
    runTest(testDispatcher) {
      val albums =
        listOf(
          AlbumEntity(artist = "Iron Maiden", album = "Powerslave", dateAdded = 1000L),
          AlbumEntity(artist = "Iron Butterfly", album = "In-A-Gadda-Da-Vida", dateAdded = 1000L),
          AlbumEntity(artist = "Black Sabbath", album = "Iron Man", dateAdded = 1000L),
          AlbumEntity(artist = "Metallica", album = "Master of Puppets", dateAdded = 1000L)
        )
      dao.insert(albums)

      val result = repository.search("Iron").asSnapshot()

      // Should return albums containing "Iron" in either artist or album name, sorted by album name
      assertThat(
        result.map {
          it.album
        }
      ).containsExactly("In-A-Gadda-Da-Vida", "Iron Man", "Powerslave").inOrder()
    }
  }

  @Test
  fun searchShouldReturnBothArtistAndAlbumMatches() {
    runTest(testDispatcher) {
      val albums =
        listOf(
          AlbumEntity(artist = "Pink Floyd", album = "Dark Side of the Moon", dateAdded = 1000L),
          AlbumEntity(artist = "Led Zeppelin", album = "Pink Floyd Tribute", dateAdded = 1000L),
          AlbumEntity(artist = "The Beatles", album = "Abbey Road", dateAdded = 1000L)
        )
      dao.insert(albums)

      val result = repository.search("Pink").asSnapshot()

      // Should return both albums: one by artist match, one by album match
      assertThat(
        result.map {
          it.album
        }
      ).containsExactly("Dark Side of the Moon", "Pink Floyd Tribute").inOrder()
    }
  }

  @Test
  fun searchShouldBeCaseInsensitiveForArtistNames() {
    runTest(testDispatcher) {
      val albums =
        listOf(
          AlbumEntity(artist = "METALLICA", album = "Master of Puppets", dateAdded = 1000L),
          AlbumEntity(artist = "metallica", album = "Black Album", dateAdded = 1000L),
          AlbumEntity(artist = "Iron Maiden", album = "Powerslave", dateAdded = 1000L)
        )
      dao.insert(albums)

      val result = repository.search("metallica").asSnapshot()

      assertThat(
        result.map {
          it.album
        }
      ).containsExactly("Black Album", "Master of Puppets").inOrder()
    }
  }

  @Test
  fun getByIdShouldReturnAlbumWhenExists() {
    runTest(testDispatcher) {
      val album = AlbumEntity(artist = "Artist1", album = "Album1", dateAdded = 1000L)
      dao.insert(listOf(album))
      val insertedAlbum = dao.all().first()

      val result = repository.getById(insertedAlbum.id)

      assertThat(result).isNotNull()
      assertThat(result!!.album).isEqualTo("Album1")
      assertThat(result.id).isEqualTo(insertedAlbum.id)
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
  fun getAlbumsByArtistShouldReturnAlbumsSortedByYear() {
    runTest(testDispatcher) {
      // Given: Albums for different artists
      val albums =
        listOf(
          AlbumEntity(artist = "Artist1", album = "Newer Album", dateAdded = 1000L),
          AlbumEntity(artist = "Artist1", album = "Older Album", dateAdded = 1000L),
          AlbumEntity(artist = "Artist1", album = "Middle Album", dateAdded = 1000L),
          AlbumEntity(artist = "Artist2", album = "Other Album", dateAdded = 1000L)
        )
      dao.insert(albums)

      // And: Tracks with different years
      val tracks =
        listOf(
          TrackEntity(
            artist = "Artist1",
            albumArtist = "Artist1",
            album = "Newer Album",
            title = "Track1",
            src = "track1.mp3",
            trackno = 1,
            disc = 1,
            genre = "Rock",
            year = "2023",
            sortableYear = "2023",
            dateAdded = 1000L
          ),
          TrackEntity(
            artist = "Artist1",
            albumArtist = "Artist1",
            album = "Older Album",
            title = "Track2",
            src = "track2.mp3",
            trackno = 1,
            disc = 1,
            genre = "Rock",
            year = "2010",
            sortableYear = "2010",
            dateAdded = 1000L
          ),
          TrackEntity(
            artist = "Artist1",
            albumArtist = "Artist1",
            album = "Middle Album",
            title = "Track3",
            src = "track3.mp3",
            trackno = 1,
            disc = 1,
            genre = "Rock",
            year = "2015",
            sortableYear = "2015",
            dateAdded = 1000L
          ),
          TrackEntity(
            artist = "Artist2",
            albumArtist = "Artist2",
            album = "Other Album",
            title = "Track4",
            src = "track4.mp3",
            trackno = 1,
            disc = 1,
            genre = "Rock",
            year = "2020",
            sortableYear = "2020",
            dateAdded = 1000L
          )
        )
      trackDao.insertAll(tracks)

      // When: Get albums by artist
      val result = repository.getAlbumsByArtist(
        "Artist1",
        AlbumSortField.YEAR,
        SortOrder.ASC
      ).asSnapshot()

      // Then: Should be sorted by year (oldest first)
      assertThat(result.map { it.album }).containsExactly(
        "Older Album",
        "Middle Album",
        "Newer Album"
      ).inOrder()
    }
  }

  @Test
  fun getAlbumsByArtistShouldPutUnknownYearsAtEnd() {
    runTest(testDispatcher) {
      // Given: Albums where some have unknown years
      val albums =
        listOf(
          AlbumEntity(artist = "Artist1", album = "Known Year Album", dateAdded = 1000L),
          AlbumEntity(artist = "Artist1", album = "Unknown Year Album", dateAdded = 1000L),
          AlbumEntity(artist = "Artist1", album = "Earlier Album", dateAdded = 1000L)
        )
      dao.insert(albums)

      val tracks =
        listOf(
          TrackEntity(
            artist = "Artist1",
            albumArtist = "Artist1",
            album = "Known Year Album",
            title = "Track1",
            src = "track1.mp3",
            trackno = 1,
            disc = 1,
            genre = "Rock",
            year = "2020",
            sortableYear = "2020",
            dateAdded = 1000L
          ),
          TrackEntity(
            artist = "Artist1",
            albumArtist = "Artist1",
            album = "Unknown Year Album",
            title = "Track2",
            src = "track2.mp3",
            trackno = 1,
            disc = 1,
            genre = "Rock",
            year = "",
            sortableYear = "",
            dateAdded = 1000L
          ),
          TrackEntity(
            artist = "Artist1",
            albumArtist = "Artist1",
            album = "Earlier Album",
            title = "Track3",
            src = "track3.mp3",
            trackno = 1,
            disc = 1,
            genre = "Rock",
            year = "2015",
            sortableYear = "2015",
            dateAdded = 1000L
          )
        )
      trackDao.insertAll(tracks)

      // When: Get albums by artist
      val result = repository.getAlbumsByArtist(
        "Artist1",
        AlbumSortField.YEAR,
        SortOrder.ASC
      ).asSnapshot()

      // Then: Known years first (sorted), then unknown years at end
      assertThat(result.map { it.album }).containsExactly(
        "Earlier Album",
        "Known Year Album",
        "Unknown Year Album"
      ).inOrder()
    }
  }

  @Test
  fun getAlbumsByArtistShouldReturnEmptyWhenNoMatches() {
    runTest(testDispatcher) {
      val albums =
        listOf(
          AlbumEntity(artist = "Artist1", album = "Album1", dateAdded = 1000L),
          AlbumEntity(artist = "Artist2", album = "Album2", dateAdded = 1000L)
        )
      dao.insert(albums)

      val result = repository.getAlbumsByArtist(
        "Artist3",
        AlbumSortField.YEAR,
        SortOrder.ASC
      ).asSnapshot()

      assertThat(result).isEmpty()
    }
  }

  @Test
  fun getRemoteShouldFetchAndStoreNewAlbums() {
    runTest(testDispatcher) {
      val remoteAlbums =
        listOf(
          AlbumDto(artist = "Artist1", album = "Album1"),
          AlbumDto(artist = "Artist2", album = "Album2")
        )
      every { libraryApi.getAlbums(any()) } returns flowOf(remoteAlbums)

      repository.getRemote(null)

      val storedAlbums = dao.all()
      assertThat(storedAlbums.map { it.album }).containsExactly("Album1", "Album2")
    }
  }

  @Test
  fun getRemoteShouldUpdateExistingAlbums() {
    runTest(testDispatcher) {
      val existingAlbum = AlbumEntity(artist = "Artist1", album = "Album1", dateAdded = 500L)
      dao.insert(listOf(existingAlbum))
      val insertedId = dao.all().first { it.album == "Album1" }.id

      val remoteAlbums = listOf(AlbumDto(artist = "Artist1", album = "Album1"))
      every { libraryApi.getAlbums(any()) } returns flowOf(remoteAlbums)

      repository.getRemote(null)

      val updatedAlbums = dao.all()
      assertThat(updatedAlbums).hasSize(1)
      assertThat(updatedAlbums.first().id).isEqualTo(insertedId)
      assertThat(updatedAlbums.first().dateAdded).isGreaterThan(500L)
    }
  }

  @Test
  fun getRemoteShouldRemovePreviousEntries() {
    runTest(testDispatcher) {
      val oldAlbum = AlbumEntity(artist = "OldArtist", album = "OldAlbum", dateAdded = 500L)
      dao.insert(listOf(oldAlbum))

      val remoteAlbums = listOf(AlbumDto(artist = "NewArtist", album = "NewAlbum"))
      every { libraryApi.getAlbums(any()) } returns flowOf(remoteAlbums)

      repository.getRemote(null)

      val storedAlbums = dao.all()
      assertThat(storedAlbums).hasSize(1)
      assertThat(storedAlbums.first().album).isEqualTo("NewAlbum")
    }
  }

  @Test
  fun getRemoteShouldHandleProgressCallback() {
    runTest(testDispatcher) {
      val progress: Progress = mockk(relaxed = true)
      val remoteAlbums = listOf(AlbumDto(artist = "Artist1", album = "Album1"))
      every { libraryApi.getAlbums(progress) } returns flowOf(remoteAlbums)

      repository.getRemote(progress)

      verify { libraryApi.getAlbums(progress) }
    }
  }

  @Test
  fun getRemoteShouldHandleMixOfNewAndExistingAlbums() {
    runTest(testDispatcher) {
      val existingAlbums =
        listOf(
          AlbumEntity(artist = "Artist1", album = "Album1", dateAdded = 500L),
          AlbumEntity(artist = "Artist2", album = "Album2", dateAdded = 500L)
        )
      dao.insert(existingAlbums)
      val album1Id = dao.all().first { it.album == "Album1" }.id

      val remoteAlbums =
        listOf(
          AlbumDto(artist = "Artist1", album = "Album1"),
          AlbumDto(artist = "Artist3", album = "Album3")
        )
      every { libraryApi.getAlbums(any()) } returns flowOf(remoteAlbums)

      repository.getRemote(null)

      val storedAlbums = dao.all().sortedBy { it.album }
      assertThat(storedAlbums).hasSize(2)
      assertThat(storedAlbums.map { it.album }).containsExactly("Album1", "Album3")
      assertThat(storedAlbums.first { it.album == "Album1" }.id).isEqualTo(album1Id)
    }
  }

  @Test
  fun updateCoversShouldUpdateAlbumCovers() {
    runTest(testDispatcher) {
      val albums =
        listOf(
          AlbumEntity(artist = "Artist1", album = "Album1", dateAdded = 1000L),
          AlbumEntity(artist = "Artist2", album = "Album2", dateAdded = 1000L)
        )
      dao.insert(albums)

      val covers =
        listOf(
          AlbumCover(artist = "Artist1", album = "Album1", hash = "hash1"),
          AlbumCover(artist = "Artist2", album = "Album2", hash = "hash2")
        )

      repository.updateCovers(covers)

      val updatedAlbums = dao.all()
      assertThat(updatedAlbums.find { it.album == "Album1" }?.cover).isEqualTo("hash1")
      assertThat(updatedAlbums.find { it.album == "Album2" }?.cover).isEqualTo("hash2")
    }
  }

  @Test
  fun updateCoversShouldSkipNullOrEmptyHashes() {
    runTest(testDispatcher) {
      val albums =
        listOf(
          AlbumEntity(artist = "Artist1", album = "Album1", dateAdded = 1000L),
          AlbumEntity(artist = "Artist2", album = "Album2", dateAdded = 1000L)
        )
      dao.insert(albums)

      val covers =
        listOf(
          AlbumCover(artist = "Artist1", album = "Album1", hash = "hash1"),
          AlbumCover(artist = "Artist2", album = "Album2", hash = null)
        )

      repository.updateCovers(covers)

      val updatedAlbums = dao.all()
      assertThat(updatedAlbums.find { it.album == "Album1" }?.cover).isEqualTo("hash1")
      assertThat(updatedAlbums.find { it.album == "Album2" }?.cover).isNull()
    }
  }

  @Test
  fun getCoversShouldReturnAllCovers() {
    runTest(testDispatcher) {
      val albums =
        listOf(
          AlbumEntity(artist = "Artist1", album = "Album1", cover = "hash1", dateAdded = 1000L),
          AlbumEntity(artist = "Artist2", album = "Album2", cover = "hash2", dateAdded = 1000L)
        )
      dao.insert(albums)

      val covers = repository.getCovers()

      assertThat(covers).hasSize(2)
      assertThat(covers.map { it.hash }).containsExactly("hash1", "hash2")
    }
  }

  @Test
  fun getCoversShouldReturnEmptyWhenNoCovers() {
    runTest(testDispatcher) {
      val covers = repository.getCovers()

      assertThat(covers).isEmpty()
    }
  }

  @Test
  fun coverCountShouldReturnCorrectCount() {
    runTest(testDispatcher) {
      val albums =
        listOf(
          AlbumEntity(artist = "Artist1", album = "Album1", cover = "hash1", dateAdded = 1000L),
          AlbumEntity(artist = "Artist2", album = "Album2", cover = "hash2", dateAdded = 1000L),
          AlbumEntity(artist = "Artist3", album = "Album3", cover = null, dateAdded = 1000L)
        )
      dao.insert(albums)

      val count = repository.coverCount()

      assertThat(count).isEqualTo(2)
    }
  }

  @Test
  fun coverCountShouldReturnZeroWhenNoCovers() {
    runTest(testDispatcher) {
      val albums =
        listOf(
          AlbumEntity(artist = "Artist1", album = "Album1", cover = null, dateAdded = 1000L),
          AlbumEntity(artist = "Artist2", album = "Album2", cover = null, dateAdded = 1000L)
        )
      dao.insert(albums)

      val count = repository.coverCount()

      assertThat(count).isEqualTo(0)
    }
  }

  @Test
  fun getAllShouldSortByNameAsc() {
    runTest(testDispatcher) {
      val albums =
        listOf(
          AlbumEntity(artist = "Artist1", album = "Zebra", dateAdded = 1000L),
          AlbumEntity(artist = "Artist2", album = "Alpha", dateAdded = 1000L),
          AlbumEntity(artist = "Artist3", album = "Middle", dateAdded = 1000L)
        )
      dao.insert(albums)

      val result = repository.getAll(AlbumSortField.NAME, SortOrder.ASC).asSnapshot()

      assertThat(result.map { it.album }).containsExactly("Alpha", "Middle", "Zebra").inOrder()
    }
  }

  @Test
  fun getAllShouldSortByNameDesc() {
    runTest(testDispatcher) {
      val albums =
        listOf(
          AlbumEntity(artist = "Artist1", album = "Zebra", dateAdded = 1000L),
          AlbumEntity(artist = "Artist2", album = "Alpha", dateAdded = 1000L),
          AlbumEntity(artist = "Artist3", album = "Middle", dateAdded = 1000L)
        )
      dao.insert(albums)

      val result = repository.getAll(AlbumSortField.NAME, SortOrder.DESC).asSnapshot()

      assertThat(result.map { it.album }).containsExactly("Zebra", "Middle", "Alpha").inOrder()
    }
  }

  @Test
  fun getAllShouldSortByArtistAscIgnoringThePrefix() {
    runTest(testDispatcher) {
      val albums =
        listOf(
          AlbumEntity(artist = "The Beatles", album = "Abbey Road", dateAdded = 1000L),
          AlbumEntity(artist = "AC/DC", album = "Back in Black", dateAdded = 1000L),
          AlbumEntity(artist = "Zebra Band", album = "Zebra Album", dateAdded = 1000L)
        )
      dao.insert(albums)

      val result = repository.getAll(AlbumSortField.ARTIST, SortOrder.ASC).asSnapshot()

      // AC/DC -> A, The Beatles -> B (ignoring "The "), Zebra Band -> Z
      assertThat(result.map { it.artist })
        .containsExactly("AC/DC", "The Beatles", "Zebra Band")
        .inOrder()
    }
  }

  @Test
  fun getAllShouldSortByArtistDescIgnoringThePrefix() {
    runTest(testDispatcher) {
      val albums =
        listOf(
          AlbumEntity(artist = "The Beatles", album = "Abbey Road", dateAdded = 1000L),
          AlbumEntity(artist = "AC/DC", album = "Back in Black", dateAdded = 1000L),
          AlbumEntity(artist = "Zebra Band", album = "Zebra Album", dateAdded = 1000L)
        )
      dao.insert(albums)

      val result = repository.getAll(AlbumSortField.ARTIST, SortOrder.DESC).asSnapshot()

      assertThat(result.map { it.artist })
        .containsExactly("Zebra Band", "The Beatles", "AC/DC")
        .inOrder()
    }
  }

  @Test
  fun getAllShouldSortByYearAscWithUnknownYearsAtEnd() {
    runTest(testDispatcher) {
      val albums =
        listOf(
          AlbumEntity(artist = "Artist1", album = "Album 2020", dateAdded = 1000L),
          AlbumEntity(artist = "Artist2", album = "Album Unknown", dateAdded = 1000L),
          AlbumEntity(artist = "Artist3", album = "Album 2010", dateAdded = 1000L)
        )
      dao.insert(albums)

      val tracks =
        listOf(
          TrackEntity(
            artist = "Artist1", albumArtist = "Artist1", album = "Album 2020",
            title = "Track1", src = "track1.mp3", trackno = 1, disc = 1,
            genre = "Rock", year = "2020", sortableYear = "2020", dateAdded = 1000L
          ),
          TrackEntity(
            artist = "Artist2", albumArtist = "Artist2", album = "Album Unknown",
            title = "Track2", src = "track2.mp3", trackno = 1, disc = 1,
            genre = "Rock", year = "", sortableYear = "", dateAdded = 1000L
          ),
          TrackEntity(
            artist = "Artist3", albumArtist = "Artist3", album = "Album 2010",
            title = "Track3", src = "track3.mp3", trackno = 1, disc = 1,
            genre = "Rock", year = "2010", sortableYear = "2010", dateAdded = 1000L
          )
        )
      trackDao.insertAll(tracks)

      val result = repository.getAll(AlbumSortField.YEAR, SortOrder.ASC).asSnapshot()

      assertThat(result.map { it.album }).containsExactly(
        "Album 2010",
        "Album 2020",
        "Album Unknown"
      ).inOrder()
    }
  }

  @Test
  fun getAllShouldSortByYearDescWithUnknownYearsAtEnd() {
    runTest(testDispatcher) {
      val albums =
        listOf(
          AlbumEntity(artist = "Artist1", album = "Album 2020", dateAdded = 1000L),
          AlbumEntity(artist = "Artist2", album = "Album Unknown", dateAdded = 1000L),
          AlbumEntity(artist = "Artist3", album = "Album 2010", dateAdded = 1000L)
        )
      dao.insert(albums)

      val tracks =
        listOf(
          TrackEntity(
            artist = "Artist1", albumArtist = "Artist1", album = "Album 2020",
            title = "Track1", src = "track1.mp3", trackno = 1, disc = 1,
            genre = "Rock", year = "2020", sortableYear = "2020", dateAdded = 1000L
          ),
          TrackEntity(
            artist = "Artist2", albumArtist = "Artist2", album = "Album Unknown",
            title = "Track2", src = "track2.mp3", trackno = 1, disc = 1,
            genre = "Rock", year = "", sortableYear = "", dateAdded = 1000L
          ),
          TrackEntity(
            artist = "Artist3", albumArtist = "Artist3", album = "Album 2010",
            title = "Track3", src = "track3.mp3", trackno = 1, disc = 1,
            genre = "Rock", year = "2010", sortableYear = "2010", dateAdded = 1000L
          )
        )
      trackDao.insertAll(tracks)

      val result = repository.getAll(AlbumSortField.YEAR, SortOrder.DESC).asSnapshot()

      assertThat(result.map { it.album }).containsExactly(
        "Album 2020",
        "Album 2010",
        "Album Unknown"
      ).inOrder()
    }
  }

  @Test
  fun searchShouldSortByNameAsc() {
    runTest(testDispatcher) {
      val albums =
        listOf(
          AlbumEntity(artist = "Rock Artist", album = "Zebra Rock", dateAdded = 1000L),
          AlbumEntity(artist = "Rock Band", album = "Alpha Rock", dateAdded = 1000L),
          AlbumEntity(artist = "Jazz Artist", album = "Jazz Album", dateAdded = 1000L)
        )
      dao.insert(albums)

      val result = repository.search("Rock", AlbumSortField.NAME, SortOrder.ASC).asSnapshot()

      assertThat(result.map { it.album }).containsExactly("Alpha Rock", "Zebra Rock").inOrder()
    }
  }

  @Test
  fun searchShouldSortByNameDesc() {
    runTest(testDispatcher) {
      val albums =
        listOf(
          AlbumEntity(artist = "Rock Artist", album = "Zebra Rock", dateAdded = 1000L),
          AlbumEntity(artist = "Rock Band", album = "Alpha Rock", dateAdded = 1000L),
          AlbumEntity(artist = "Jazz Artist", album = "Jazz Album", dateAdded = 1000L)
        )
      dao.insert(albums)

      val result = repository.search("Rock", AlbumSortField.NAME, SortOrder.DESC).asSnapshot()

      assertThat(result.map { it.album }).containsExactly("Zebra Rock", "Alpha Rock").inOrder()
    }
  }

  @Test
  fun searchShouldSortByArtistAsc() {
    runTest(testDispatcher) {
      val albums =
        listOf(
          AlbumEntity(artist = "The Rock Band", album = "Album1", dateAdded = 1000L),
          AlbumEntity(artist = "Alpha Rock", album = "Album2", dateAdded = 1000L),
          AlbumEntity(artist = "Jazz Artist", album = "Jazz Album", dateAdded = 1000L)
        )
      dao.insert(albums)

      val result = repository.search("Rock", AlbumSortField.ARTIST, SortOrder.ASC).asSnapshot()

      // Alpha Rock -> A, The Rock Band -> R (ignoring "The ")
      assertThat(result.map { it.artist }).containsExactly("Alpha Rock", "The Rock Band").inOrder()
    }
  }

  @Test
  fun searchShouldSortByYearAscWithUnknownYearsAtEnd() {
    runTest(testDispatcher) {
      val albums =
        listOf(
          AlbumEntity(artist = "Rock Artist", album = "Rock 2020", dateAdded = 1000L),
          AlbumEntity(artist = "Rock Band", album = "Rock Unknown", dateAdded = 1000L),
          AlbumEntity(artist = "Rock Group", album = "Rock 2010", dateAdded = 1000L)
        )
      dao.insert(albums)

      val tracks =
        listOf(
          TrackEntity(
            artist = "Rock Artist", albumArtist = "Rock Artist", album = "Rock 2020",
            title = "Track1", src = "track1.mp3", trackno = 1, disc = 1,
            genre = "Rock", year = "2020", sortableYear = "2020", dateAdded = 1000L
          ),
          TrackEntity(
            artist = "Rock Band", albumArtist = "Rock Band", album = "Rock Unknown",
            title = "Track2", src = "track2.mp3", trackno = 1, disc = 1,
            genre = "Rock", year = "", sortableYear = "", dateAdded = 1000L
          ),
          TrackEntity(
            artist = "Rock Group", albumArtist = "Rock Group", album = "Rock 2010",
            title = "Track3", src = "track3.mp3", trackno = 1, disc = 1,
            genre = "Rock", year = "2010", sortableYear = "2010", dateAdded = 1000L
          )
        )
      trackDao.insertAll(tracks)

      val result = repository.search("Rock", AlbumSortField.YEAR, SortOrder.ASC).asSnapshot()

      assertThat(result.map { it.album }).containsExactly(
        "Rock 2010",
        "Rock 2020",
        "Rock Unknown"
      ).inOrder()
    }
  }
}
