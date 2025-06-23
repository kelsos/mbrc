@file:OptIn(ExperimentalCoroutinesApi::class)

package com.kelsos.mbrc.features.library.albums

import androidx.paging.testing.asSnapshot
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import com.kelsos.mbrc.common.data.Progress
import com.kelsos.mbrc.data.Database
import com.kelsos.mbrc.features.library.tracks.TrackDao
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

@RunWith(AndroidJUnit4::class)
class AlbumRepositoryTest {
  private lateinit var database: Database
  private lateinit var repository: AlbumRepository
  private lateinit var dao: AlbumDao
  private lateinit var trackDao: TrackDao
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
    dao = database.albumDao()
    trackDao = database.trackDao()
    repository = AlbumRepositoryImpl(dao, api, testDispatchers)
  }

  @After
  fun tearDown() {
    database.close()
  }

  @Test
  fun count_shouldReturnCorrectCount() {
    runTest(testDispatcher) {
      val albums =
        listOf(
          AlbumEntity(artist = "Artist1", album = "Album1", dateAdded = 1000L),
          AlbumEntity(artist = "Artist2", album = "Album2", dateAdded = 1000L),
          AlbumEntity(artist = "Artist3", album = "Album3", dateAdded = 1000L),
        )
      dao.insert(albums)

      val count = repository.count()

      assertThat(count).isEqualTo(3)
    }
  }

  @Test
  fun count_shouldReturnZeroWhenEmpty() {
    runTest(testDispatcher) {
      val count = repository.count()

      assertThat(count).isEqualTo(0)
    }
  }

  @Test
  fun getAll_shouldReturnAllAlbumsSorted() {
    runTest(testDispatcher) {
      val albums =
        listOf(
          AlbumEntity(artist = "Artist1", album = "Album C", dateAdded = 1000L),
          AlbumEntity(artist = "Artist2", album = "Album A", dateAdded = 1000L),
          AlbumEntity(artist = "Artist3", album = "Album B", dateAdded = 1000L),
        )
      dao.insert(albums)

      val result = repository.getAll().asSnapshot()

      assertThat(result.map { it.album }).containsExactly("Album A", "Album B", "Album C").inOrder()
    }
  }

  @Test
  fun getAll_shouldReturnEmptyWhenNoAlbums() {
    runTest(testDispatcher) {
      val result = repository.getAll().asSnapshot()

      assertThat(result).isEmpty()
    }
  }

  @Test
  fun search_shouldReturnMatchingAlbums() {
    runTest(testDispatcher) {
      val albums =
        listOf(
          AlbumEntity(artist = "Artist1", album = "Rock Album", dateAdded = 1000L),
          AlbumEntity(artist = "Artist2", album = "Pop Rock", dateAdded = 1000L),
          AlbumEntity(artist = "Artist3", album = "Jazz", dateAdded = 1000L),
          AlbumEntity(artist = "Artist4", album = "Hard Rock", dateAdded = 1000L),
        )
      dao.insert(albums)

      val result = repository.search("Rock").asSnapshot()

      // The DAO sorts by album name, so the order is alphabetical
      assertThat(result.map { it.album }).containsExactly("Rock Album", "Pop Rock", "Hard Rock").inOrder()
    }
  }

  @Test
  fun search_shouldReturnEmptyWhenNoMatches() {
    runTest(testDispatcher) {
      val albums =
        listOf(
          AlbumEntity(artist = "Artist1", album = "Rock Album", dateAdded = 1000L),
          AlbumEntity(artist = "Artist2", album = "Pop", dateAdded = 1000L),
        )
      dao.insert(albums)

      val result = repository.search("Classical").asSnapshot()

      assertThat(result).isEmpty()
    }
  }

  @Test
  fun search_shouldBeCaseInsensitive() {
    runTest(testDispatcher) {
      val albums =
        listOf(
          AlbumEntity(artist = "Artist1", album = "Rock Album", dateAdded = 1000L),
          AlbumEntity(artist = "Artist2", album = "JAZZ", dateAdded = 1000L),
          AlbumEntity(artist = "Artist3", album = "pop", dateAdded = 1000L),
        )
      dao.insert(albums)

      val result = repository.search("rock").asSnapshot()

      assertThat(result.map { it.album }).containsExactly("Rock Album")
    }
  }

  @Test
  fun getById_shouldReturnAlbumWhenExists() {
    runTest(testDispatcher) {
      val album = AlbumEntity(artist = "Artist1", album = "Album1", dateAdded = 1000L)
      dao.insert(listOf(album))
      val insertedAlbum = dao.all().first()

      val result = repository.getById(insertedAlbum.id!!)

      assertThat(result).isNotNull()
      assertThat(result!!.album).isEqualTo("Album1")
      assertThat(result.id).isEqualTo(insertedAlbum.id)
    }
  }

  @Test
  fun getById_shouldReturnNullWhenNotExists() {
    runTest(testDispatcher) {
      val result = repository.getById(999L)

      assertThat(result).isNull()
    }
  }

  @Test
  fun getAlbumsByArtist_shouldReturnAlbumsForArtist() {
    runTest(testDispatcher) {
      // Given: Albums for different artists
      val albums =
        listOf(
          AlbumEntity(artist = "Artist1", album = "Album1", dateAdded = 1000L),
          AlbumEntity(artist = "Artist1", album = "Album2", dateAdded = 1000L),
          AlbumEntity(artist = "Artist2", album = "Album3", dateAdded = 1000L),
        )
      dao.insert(albums)

      // And: Tracks for those albums
      val tracks =
        listOf(
          TrackEntity(
            artist = "Artist1",
            albumArtist = "Artist1",
            album = "Album1",
            title = "Track1",
            dateAdded = 1000L,
          ),
          TrackEntity(
            artist = "Artist1",
            albumArtist = "Artist1",
            album = "Album2",
            title = "Track2",
            dateAdded = 1000L,
          ),
          TrackEntity(
            artist = "Artist2",
            albumArtist = "Artist2",
            album = "Album3",
            title = "Track3",
            dateAdded = 1000L,
          ),
        )
      trackDao.insertAll(tracks)

      // When: Get albums by artist
      val result = repository.getAlbumsByArtist("Artist1").asSnapshot()

      // Then: Should only include albums for that artist
      assertThat(result.map { it.album }).containsExactly("Album1", "Album2")
    }
  }

  @Test
  fun getAlbumsByArtist_shouldReturnEmptyWhenNoMatches() {
    runTest(testDispatcher) {
      val albums =
        listOf(
          AlbumEntity(artist = "Artist1", album = "Album1", dateAdded = 1000L),
          AlbumEntity(artist = "Artist2", album = "Album2", dateAdded = 1000L),
        )
      dao.insert(albums)

      val result = repository.getAlbumsByArtist("Artist3").asSnapshot()

      assertThat(result).isEmpty()
    }
  }

  @Test
  fun getRemote_shouldFetchAndStoreNewAlbums() {
    runTest(testDispatcher) {
      val remoteAlbums =
        listOf(
          AlbumDto(artist = "Artist1", album = "Album1"),
          AlbumDto(artist = "Artist2", album = "Album2"),
        )
      coEvery {
        api.getAllPages(Protocol.LibraryBrowseAlbums, AlbumDto::class, any())
      } returns flowOf(remoteAlbums)

      repository.getRemote(null)

      val storedAlbums = dao.all()
      assertThat(storedAlbums.map { it.album }).containsExactly("Album1", "Album2")
    }
  }

  @Test
  fun getRemote_shouldUpdateExistingAlbums() {
    runTest(testDispatcher) {
      val existingAlbum = AlbumEntity(artist = "Artist1", album = "Album1", dateAdded = 500L)
      dao.insert(listOf(existingAlbum))
      val insertedId = dao.all().first { it.album == "Album1" }.id

      val remoteAlbums = listOf(AlbumDto(artist = "Artist1", album = "Album1"))
      coEvery {
        api.getAllPages(Protocol.LibraryBrowseAlbums, AlbumDto::class, any())
      } returns flowOf(remoteAlbums)

      repository.getRemote(null)

      val updatedAlbums = dao.all()
      assertThat(updatedAlbums).hasSize(1)
      assertThat(updatedAlbums.first().id).isEqualTo(insertedId)
      assertThat(updatedAlbums.first().dateAdded).isGreaterThan(500L)
    }
  }

  @Test
  fun getRemote_shouldRemovePreviousEntries() {
    runTest(testDispatcher) {
      val oldAlbum = AlbumEntity(artist = "OldArtist", album = "OldAlbum", dateAdded = 500L)
      dao.insert(listOf(oldAlbum))

      val remoteAlbums = listOf(AlbumDto(artist = "NewArtist", album = "NewAlbum"))
      coEvery {
        api.getAllPages(Protocol.LibraryBrowseAlbums, AlbumDto::class, any())
      } returns flowOf(remoteAlbums)

      repository.getRemote(null)

      val storedAlbums = dao.all()
      assertThat(storedAlbums).hasSize(1)
      assertThat(storedAlbums.first().album).isEqualTo("NewAlbum")
    }
  }

  @Test
  fun getRemote_shouldHandleProgressCallback() {
    runTest(testDispatcher) {
      val progress: Progress = mockk(relaxed = true)
      val remoteAlbums = listOf(AlbumDto(artist = "Artist1", album = "Album1"))
      coEvery {
        api.getAllPages(Protocol.LibraryBrowseAlbums, AlbumDto::class, progress)
      } returns flowOf(remoteAlbums)

      repository.getRemote(progress)

      @Suppress("IgnoredReturnValue")
      verify { api.getAllPages(Protocol.LibraryBrowseAlbums, AlbumDto::class, progress) }
    }
  }

  @Test
  fun getRemote_shouldHandleMixOfNewAndExistingAlbums() {
    runTest(testDispatcher) {
      val existingAlbums =
        listOf(
          AlbumEntity(artist = "Artist1", album = "Album1", dateAdded = 500L),
          AlbumEntity(artist = "Artist2", album = "Album2", dateAdded = 500L),
        )
      dao.insert(existingAlbums)
      val album1Id = dao.all().first { it.album == "Album1" }.id

      val remoteAlbums =
        listOf(
          AlbumDto(artist = "Artist1", album = "Album1"),
          AlbumDto(artist = "Artist3", album = "Album3"),
        )
      coEvery {
        api.getAllPages(Protocol.LibraryBrowseAlbums, AlbumDto::class, any())
      } returns flowOf(remoteAlbums)

      repository.getRemote(null)

      val storedAlbums = dao.all().sortedBy { it.album }
      assertThat(storedAlbums).hasSize(2)
      assertThat(storedAlbums.map { it.album }).containsExactly("Album1", "Album3")
      assertThat(storedAlbums.first { it.album == "Album1" }.id).isEqualTo(album1Id)
    }
  }

  @Test
  fun updateCovers_shouldUpdateAlbumCovers() {
    runTest(testDispatcher) {
      val albums =
        listOf(
          AlbumEntity(artist = "Artist1", album = "Album1", dateAdded = 1000L),
          AlbumEntity(artist = "Artist2", album = "Album2", dateAdded = 1000L),
        )
      dao.insert(albums)

      val covers =
        listOf(
          AlbumCover(artist = "Artist1", album = "Album1", hash = "hash1"),
          AlbumCover(artist = "Artist2", album = "Album2", hash = "hash2"),
        )

      repository.updateCovers(covers)

      val updatedAlbums = dao.all()
      assertThat(updatedAlbums.find { it.album == "Album1" }?.cover).isEqualTo("hash1")
      assertThat(updatedAlbums.find { it.album == "Album2" }?.cover).isEqualTo("hash2")
    }
  }

  @Test
  fun updateCovers_shouldSkipNullOrEmptyHashes() {
    runTest(testDispatcher) {
      val albums =
        listOf(
          AlbumEntity(artist = "Artist1", album = "Album1", dateAdded = 1000L),
          AlbumEntity(artist = "Artist2", album = "Album2", dateAdded = 1000L),
        )
      dao.insert(albums)

      val covers =
        listOf(
          AlbumCover(artist = "Artist1", album = "Album1", hash = "hash1"),
          AlbumCover(artist = "Artist2", album = "Album2", hash = null),
        )

      repository.updateCovers(covers)

      val updatedAlbums = dao.all()
      assertThat(updatedAlbums.find { it.album == "Album1" }?.cover).isEqualTo("hash1")
      assertThat(updatedAlbums.find { it.album == "Album2" }?.cover).isNull()
    }
  }

  @Test
  fun getCovers_shouldReturnAllCovers() {
    runTest(testDispatcher) {
      val albums =
        listOf(
          AlbumEntity(artist = "Artist1", album = "Album1", cover = "hash1", dateAdded = 1000L),
          AlbumEntity(artist = "Artist2", album = "Album2", cover = "hash2", dateAdded = 1000L),
        )
      dao.insert(albums)

      val covers = repository.getCovers()

      assertThat(covers).hasSize(2)
      assertThat(covers.map { it.hash }).containsExactly("hash1", "hash2")
    }
  }

  @Test
  fun getCovers_shouldReturnEmptyWhenNoCovers() {
    runTest(testDispatcher) {
      val covers = repository.getCovers()

      assertThat(covers).isEmpty()
    }
  }

  @Test
  fun coverCount_shouldReturnCorrectCount() {
    runTest(testDispatcher) {
      val albums =
        listOf(
          AlbumEntity(artist = "Artist1", album = "Album1", cover = "hash1", dateAdded = 1000L),
          AlbumEntity(artist = "Artist2", album = "Album2", cover = "hash2", dateAdded = 1000L),
          AlbumEntity(artist = "Artist3", album = "Album3", cover = null, dateAdded = 1000L),
        )
      dao.insert(albums)

      val count = repository.coverCount()

      assertThat(count).isEqualTo(2)
    }
  }

  @Test
  fun coverCount_shouldReturnZeroWhenNoCovers() {
    runTest(testDispatcher) {
      val albums =
        listOf(
          AlbumEntity(artist = "Artist1", album = "Album1", cover = null, dateAdded = 1000L),
          AlbumEntity(artist = "Artist2", album = "Album2", cover = null, dateAdded = 1000L),
        )
      dao.insert(albums)

      val count = repository.coverCount()

      assertThat(count).isEqualTo(0)
    }
  }
}
