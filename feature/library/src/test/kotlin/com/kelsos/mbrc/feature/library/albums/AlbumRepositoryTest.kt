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
import com.kelsos.mbrc.core.data.library.genre.GenreDao
import com.kelsos.mbrc.core.data.library.genre.GenreEntity
import com.kelsos.mbrc.core.data.library.track.TrackDao
import com.kelsos.mbrc.core.data.library.track.TrackEntity
import com.kelsos.mbrc.core.data.test.testDatabaseModule
import com.kelsos.mbrc.core.networking.api.LibraryApi
import com.kelsos.mbrc.core.networking.dto.AlbumDto
import com.kelsos.mbrc.feature.library.deriveLibrary
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
  private val genreDao: GenreDao by inject()
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
  fun getAlbumsByArtistShouldReturnAlbumsSortedByName() {
    runTest(testDispatcher) {
      val albums =
        listOf(
          AlbumEntity(artist = "Artist1", album = "Newer Album", dateAdded = 1000L),
          AlbumEntity(artist = "Artist1", album = "Older Album", dateAdded = 1000L),
          AlbumEntity(artist = "Artist1", album = "Middle Album", dateAdded = 1000L),
          AlbumEntity(artist = "Artist2", album = "Other Album", dateAdded = 1000L)
        )
      dao.insert(albums)

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
      database.deriveLibrary()

      val result = repository.getAlbumsByArtist(
        "Artist1",
        AlbumSortField.NAME,
        SortOrder.ASC
      ).asSnapshot()

      assertThat(result.map { it.album }).containsExactly(
        "Middle Album",
        "Newer Album",
        "Older Album"
      ).inOrder()
    }
  }

  @Test
  fun getAlbumsByArtistShouldSortByNameDescending() {
    runTest(testDispatcher) {
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
      database.deriveLibrary()

      val result = repository.getAlbumsByArtist(
        "Artist1",
        AlbumSortField.NAME,
        SortOrder.ASC
      ).asSnapshot()

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
        AlbumSortField.NAME,
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

  // Genre tests
  @Test
  fun getAlbumsByGenreShouldReturnAlbumsForGenre() {
    runTest(testDispatcher) {
      val genres =
        listOf(
          GenreEntity(id = 1, genre = "Rock", dateAdded = 1000L),
          GenreEntity(id = 2, genre = "Jazz", dateAdded = 1000L)
        )
      genreDao.insertAll(genres)

      val albums =
        listOf(
          AlbumEntity(artist = "Artist1", album = "Rock Album 1", dateAdded = 1000L),
          AlbumEntity(artist = "Artist1", album = "Rock Album 2", dateAdded = 1000L),
          AlbumEntity(artist = "Artist2", album = "Jazz Album", dateAdded = 1000L)
        )
      dao.insert(albums)

      val tracks =
        listOf(
          TrackEntity(
            artist = "Artist1",
            albumArtist = "Artist1",
            album = "Rock Album 1",
            title = "Rock Track 1",
            src = "rock1.mp3",
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
            album = "Rock Album 2",
            title = "Rock Track 2",
            src = "rock2.mp3",
            trackno = 1,
            disc = 1,
            genre = "Rock",
            year = "2021",
            sortableYear = "2021",
            dateAdded = 1000L
          ),
          TrackEntity(
            artist = "Artist2",
            albumArtist = "Artist2",
            album = "Jazz Album",
            title = "Jazz Track",
            src = "jazz.mp3",
            trackno = 1,
            disc = 1,
            genre = "Jazz",
            year = "2019",
            sortableYear = "2019",
            dateAdded = 1000L
          )
        )
      trackDao.insertAll(tracks)
      database.deriveLibrary()

      val result = repository.getAlbumsByGenre(
        1L,
        AlbumSortField.NAME,
        SortOrder.ASC
      ).asSnapshot()

      assertThat(result.map { it.album }).containsExactly("Rock Album 1", "Rock Album 2").inOrder()
    }
  }

  @Test
  fun getAlbumsByGenreShouldReturnEmptyWhenNoMatchingAlbums() {
    runTest(testDispatcher) {
      val genres = listOf(GenreEntity(id = 1, genre = "Classical", dateAdded = 1000L))
      genreDao.insertAll(genres)

      val albums = listOf(AlbumEntity(artist = "Artist1", album = "Rock Album", dateAdded = 1000L))
      dao.insert(albums)

      val tracks =
        listOf(
          TrackEntity(
            artist = "Artist1",
            albumArtist = "Artist1",
            album = "Rock Album",
            title = "Track 1",
            src = "track1.mp3",
            trackno = 1,
            disc = 1,
            genre = "Rock",
            year = "2020",
            sortableYear = "2020",
            dateAdded = 1000L
          )
        )
      trackDao.insertAll(tracks)
      database.deriveLibrary()

      val result = repository.getAlbumsByGenre(
        1L,
        AlbumSortField.NAME,
        SortOrder.ASC
      ).asSnapshot()

      assertThat(result).isEmpty()
    }
  }

  @Test
  fun getAlbumsByGenreShouldSortByNameDesc() {
    runTest(testDispatcher) {
      val genres = listOf(GenreEntity(id = 1, genre = "Rock", dateAdded = 1000L))
      genreDao.insertAll(genres)

      val albums =
        listOf(
          AlbumEntity(artist = "Artist1", album = "Alpha Album", dateAdded = 1000L),
          AlbumEntity(artist = "Artist1", album = "Zebra Album", dateAdded = 1000L),
          AlbumEntity(artist = "Artist1", album = "Middle Album", dateAdded = 1000L)
        )
      dao.insert(albums)

      val tracks =
        listOf(
          TrackEntity(
            artist = "Artist1",
            albumArtist = "Artist1",
            album = "Alpha Album",
            title = "Track 1",
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
            album = "Zebra Album",
            title = "Track 2",
            src = "track2.mp3",
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
            album = "Middle Album",
            title = "Track 3",
            src = "track3.mp3",
            trackno = 1,
            disc = 1,
            genre = "Rock",
            year = "2020",
            sortableYear = "2020",
            dateAdded = 1000L
          )
        )
      trackDao.insertAll(tracks)
      database.deriveLibrary()

      val result = repository.getAlbumsByGenre(
        1L,
        AlbumSortField.NAME,
        SortOrder.DESC
      ).asSnapshot()

      assertThat(result.map { it.album })
        .containsExactly("Zebra Album", "Middle Album", "Alpha Album")
        .inOrder()
    }
  }

  @Test
  fun getAlbumsByGenreShouldSortByArtistAsc() {
    runTest(testDispatcher) {
      val genres = listOf(GenreEntity(id = 1, genre = "Rock", dateAdded = 1000L))
      genreDao.insertAll(genres)

      val albums =
        listOf(
          AlbumEntity(artist = "The Beatles", album = "Abbey Road", dateAdded = 1000L),
          AlbumEntity(artist = "AC/DC", album = "Back in Black", dateAdded = 1000L),
          AlbumEntity(artist = "Zebra Band", album = "Zebra Album", dateAdded = 1000L)
        )
      dao.insert(albums)

      val tracks =
        listOf(
          TrackEntity(
            artist = "The Beatles",
            albumArtist = "The Beatles",
            album = "Abbey Road",
            title = "Track 1",
            src = "track1.mp3",
            trackno = 1,
            disc = 1,
            genre = "Rock",
            year = "2020",
            sortableYear = "2020",
            dateAdded = 1000L
          ),
          TrackEntity(
            artist = "AC/DC",
            albumArtist = "AC/DC",
            album = "Back in Black",
            title = "Track 2",
            src = "track2.mp3",
            trackno = 1,
            disc = 1,
            genre = "Rock",
            year = "2020",
            sortableYear = "2020",
            dateAdded = 1000L
          ),
          TrackEntity(
            artist = "Zebra Band",
            albumArtist = "Zebra Band",
            album = "Zebra Album",
            title = "Track 3",
            src = "track3.mp3",
            trackno = 1,
            disc = 1,
            genre = "Rock",
            year = "2020",
            sortableYear = "2020",
            dateAdded = 1000L
          )
        )
      trackDao.insertAll(tracks)
      database.deriveLibrary()

      val result = repository.getAlbumsByGenre(
        1L,
        AlbumSortField.ARTIST,
        SortOrder.ASC
      ).asSnapshot()

      // "The" prefix is ignored, so Beatles sorts under B
      assertThat(result.map { it.artist })
        .containsExactly("AC/DC", "The Beatles", "Zebra Band")
        .inOrder()
    }
  }

  @Test
  fun getAlbumsByGenreShouldSortByArtistDesc() {
    runTest(testDispatcher) {
      val genres = listOf(GenreEntity(id = 1, genre = "Rock", dateAdded = 1000L))
      genreDao.insertAll(genres)

      val albums =
        listOf(
          AlbumEntity(artist = "The Beatles", album = "Abbey Road", dateAdded = 1000L),
          AlbumEntity(artist = "AC/DC", album = "Back in Black", dateAdded = 1000L),
          AlbumEntity(artist = "Zebra Band", album = "Zebra Album", dateAdded = 1000L)
        )
      dao.insert(albums)

      val tracks =
        listOf(
          TrackEntity(
            artist = "The Beatles",
            albumArtist = "The Beatles",
            album = "Abbey Road",
            title = "Track 1",
            src = "track1.mp3",
            trackno = 1,
            disc = 1,
            genre = "Rock",
            year = "2020",
            sortableYear = "2020",
            dateAdded = 1000L
          ),
          TrackEntity(
            artist = "AC/DC",
            albumArtist = "AC/DC",
            album = "Back in Black",
            title = "Track 2",
            src = "track2.mp3",
            trackno = 1,
            disc = 1,
            genre = "Rock",
            year = "2020",
            sortableYear = "2020",
            dateAdded = 1000L
          ),
          TrackEntity(
            artist = "Zebra Band",
            albumArtist = "Zebra Band",
            album = "Zebra Album",
            title = "Track 3",
            src = "track3.mp3",
            trackno = 1,
            disc = 1,
            genre = "Rock",
            year = "2020",
            sortableYear = "2020",
            dateAdded = 1000L
          )
        )
      trackDao.insertAll(tracks)
      database.deriveLibrary()

      val result = repository.getAlbumsByGenre(
        1L,
        AlbumSortField.ARTIST,
        SortOrder.DESC
      ).asSnapshot()

      // "The" prefix is ignored, so Beatles sorts under B
      assertThat(result.map { it.artist })
        .containsExactly("Zebra Band", "The Beatles", "AC/DC")
        .inOrder()
    }
  }

  // Sorting tests
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

      assertThat(result.map { it.artist }).containsExactly("Alpha Rock", "The Rock Band").inOrder()
    }
  }

  @Test
  fun searchShouldSortByArtistDesc() {
    runTest(testDispatcher) {
      val albums =
        listOf(
          AlbumEntity(artist = "The Rock Band", album = "Album1", dateAdded = 1000L),
          AlbumEntity(artist = "Alpha Rock", album = "Album2", dateAdded = 1000L),
          AlbumEntity(artist = "Jazz Artist", album = "Jazz Album", dateAdded = 1000L)
        )
      dao.insert(albums)

      val result = repository.search("Rock", AlbumSortField.ARTIST, SortOrder.DESC).asSnapshot()

      assertThat(result.map { it.artist }).containsExactly("The Rock Band", "Alpha Rock").inOrder()
    }
  }

  @Test
  fun getAllShouldGroupEmptyAlbumsFromDifferentArtistsIntoSingleEntry() {
    runTest(testDispatcher) {
      // Given: Multiple artists with empty album names (issue #184)
      val albums =
        listOf(
          AlbumEntity(artist = "Artist1", album = "", dateAdded = 1000L),
          AlbumEntity(artist = "Artist2", album = "", dateAdded = 2000L),
          AlbumEntity(artist = "Artist3", album = "", dateAdded = 3000L)
        )
      dao.insert(albums)

      // When: Get all albums
      val result = repository.getAll().asSnapshot()

      // Then: Empty albums should be grouped into a single entry
      assertThat(result).hasSize(1)
      assertThat(result.first().album).isEmpty()
      assertThat(result.first().artist).isEmpty()
    }
  }

  @Test
  fun getAllShouldPreserveSeparateAlbumsWithSameName() {
    runTest(testDispatcher) {
      // Given: Same album name from different artists (should remain separate)
      val albums =
        listOf(
          AlbumEntity(artist = "Artist1", album = "Greatest Hits", dateAdded = 1000L),
          AlbumEntity(artist = "Artist2", album = "Greatest Hits", dateAdded = 2000L)
        )
      dao.insert(albums)

      // When: Get all albums
      val result = repository.getAll().asSnapshot()

      // Then: Both albums should exist separately
      assertThat(result).hasSize(2)
      assertThat(result.map { it.artist }).containsExactly("Artist1", "Artist2")
    }
  }

  @Test
  fun getAllShouldHandleMixOfEmptyAndNormalAlbums() {
    runTest(testDispatcher) {
      // Given: Mix of empty and normal albums from various artists
      val albums =
        listOf(
          AlbumEntity(artist = "Artist1", album = "", dateAdded = 1000L),
          AlbumEntity(artist = "Artist2", album = "", dateAdded = 2000L),
          AlbumEntity(artist = "Artist1", album = "Album A", dateAdded = 1000L),
          AlbumEntity(artist = "Artist2", album = "Album B", dateAdded = 2000L),
          AlbumEntity(artist = "Artist3", album = "Album C", dateAdded = 3000L)
        )
      dao.insert(albums)

      // When: Get all albums
      val result = repository.getAll().asSnapshot()

      // Then: Empty albums grouped into one, normal albums preserved separately
      assertThat(result).hasSize(4)
      // Empty album appears first (empty string sorts before other strings)
      assertThat(
        result.map {
          it.album
        }
      ).containsExactly("", "Album A", "Album B", "Album C").inOrder()
    }
  }

  @Test
  fun getAllSortedByNameAscShouldGroupEmptyAlbums() {
    runTest(testDispatcher) {
      // Given: Multiple artists with empty album names
      val albums =
        listOf(
          AlbumEntity(artist = "Artist1", album = "", dateAdded = 1000L),
          AlbumEntity(artist = "Artist2", album = "", dateAdded = 2000L),
          AlbumEntity(artist = "Artist1", album = "Album A", dateAdded = 1000L)
        )
      dao.insert(albums)

      // When: Get all albums sorted by name ASC
      val result = repository.getAll(AlbumSortField.NAME, SortOrder.ASC).asSnapshot()

      // Then: Empty albums should be grouped into a single entry
      assertThat(result).hasSize(2)
      assertThat(result.first().album).isEmpty()
      assertThat(result.first().artist).isEmpty()
    }
  }

  @Test
  fun getAllSortedByNameDescShouldGroupEmptyAlbums() {
    runTest(testDispatcher) {
      // Given: Multiple artists with empty album names
      val albums =
        listOf(
          AlbumEntity(artist = "Artist1", album = "", dateAdded = 1000L),
          AlbumEntity(artist = "Artist2", album = "", dateAdded = 2000L),
          AlbumEntity(artist = "Artist1", album = "Album A", dateAdded = 1000L)
        )
      dao.insert(albums)

      // When: Get all albums sorted by name DESC
      val result = repository.getAll(AlbumSortField.NAME, SortOrder.DESC).asSnapshot()

      // Then: Empty albums should be grouped into a single entry
      assertThat(result).hasSize(2)
      assertThat(result.map { it.album }).containsExactly("Album A", "").inOrder()
    }
  }

  @Test
  fun getAllSortedByArtistAscShouldGroupEmptyAlbums() {
    runTest(testDispatcher) {
      // Given: Multiple artists with empty album names
      val albums =
        listOf(
          AlbumEntity(artist = "Artist1", album = "", dateAdded = 1000L),
          AlbumEntity(artist = "Artist2", album = "", dateAdded = 2000L),
          AlbumEntity(artist = "Artist3", album = "", dateAdded = 3000L),
          AlbumEntity(artist = "Artist1", album = "Album A", dateAdded = 1000L)
        )
      dao.insert(albums)

      // When: Get all albums sorted by artist ASC
      val result = repository.getAll(AlbumSortField.ARTIST, SortOrder.ASC).asSnapshot()

      // Then: Empty albums should be grouped into a single entry
      assertThat(result).hasSize(2)
      assertThat(result.any { it.album.isEmpty() && it.artist.isEmpty() }).isTrue()
    }
  }

  @Test
  fun getAllSortedByArtistDescShouldGroupEmptyAlbums() {
    runTest(testDispatcher) {
      // Given: Multiple artists with empty album names
      val albums =
        listOf(
          AlbumEntity(artist = "Artist1", album = "", dateAdded = 1000L),
          AlbumEntity(artist = "Artist2", album = "", dateAdded = 2000L),
          AlbumEntity(artist = "Artist1", album = "Album A", dateAdded = 1000L)
        )
      dao.insert(albums)

      // When: Get all albums sorted by artist DESC
      val result = repository.getAll(AlbumSortField.ARTIST, SortOrder.DESC).asSnapshot()

      // Then: Empty albums should be grouped into a single entry
      assertThat(result).hasSize(2)
      assertThat(result.any { it.album.isEmpty() && it.artist.isEmpty() }).isTrue()
    }
  }

  @Test
  fun searchSortedByNameShouldGroupEmptyAlbums() {
    runTest(testDispatcher) {
      // Given: Multiple artists with empty album names plus a normal album
      val albums =
        listOf(
          AlbumEntity(artist = "Rock Artist1", album = "", dateAdded = 1000L),
          AlbumEntity(artist = "Rock Artist2", album = "", dateAdded = 2000L),
          AlbumEntity(artist = "Rock Artist1", album = "Rock Album", dateAdded = 1000L)
        )
      dao.insert(albums)

      // When: Search with term matching artists, sorted by name ASC
      val result = repository.search("Rock", AlbumSortField.NAME, SortOrder.ASC).asSnapshot()

      // Then: Empty albums should be grouped into a single entry
      assertThat(result).hasSize(2)
      assertThat(result.any { it.album.isEmpty() && it.artist.isEmpty() }).isTrue()
    }
  }

  @Test
  fun getAllShouldPutEmptyAlbumsFirstInSortOrder() {
    runTest(testDispatcher) {
      // Given: Albums including empty ones
      val albums =
        listOf(
          AlbumEntity(artist = "Artist1", album = "Zulu Album", dateAdded = 1000L),
          AlbumEntity(artist = "Artist2", album = "", dateAdded = 2000L),
          AlbumEntity(artist = "Artist3", album = "Alpha Album", dateAdded = 3000L)
        )
      dao.insert(albums)

      // When: Get all albums
      val result = repository.getAll().asSnapshot()

      // Then: Empty album should appear first
      assertThat(result).hasSize(3)
      assertThat(result.first().album).isEmpty()
      assertThat(result.map { it.album }).containsExactly("", "Alpha Album", "Zulu Album").inOrder()
    }
  }
}
