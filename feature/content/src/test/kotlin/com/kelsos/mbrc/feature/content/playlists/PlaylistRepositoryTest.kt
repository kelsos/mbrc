@file:OptIn(ExperimentalCoroutinesApi::class)

package com.kelsos.mbrc.feature.content.playlists

import androidx.paging.testing.asSnapshot
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import com.kelsos.mbrc.core.common.data.Progress
import com.kelsos.mbrc.core.common.test.testDispatcher
import com.kelsos.mbrc.core.common.test.testDispatcherModule
import com.kelsos.mbrc.core.data.Database
import com.kelsos.mbrc.core.data.playlist.PlaylistDao
import com.kelsos.mbrc.core.data.playlist.PlaylistEntity
import com.kelsos.mbrc.core.data.playlist.PlaylistRepository
import com.kelsos.mbrc.core.networking.api.ContentApi
import com.kelsos.mbrc.core.networking.dto.PlaylistDto
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import java.io.IOException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flow
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
class PlaylistRepositoryTest : KoinTest {
  // Constants for test data
  private companion object {
    const val DEFAULT_DATE_ADDED = 1000L
    const val OLDER_DATE_ADDED = 500L
  }

  private val testModule =
    module {
      single<ContentApi> { mockk() }
      single {
        Room
          .inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            Database::class.java
          ).allowMainThreadQueries()
          .build()
      }
      single { get<Database>().playlistDao() }
      singleOf(::PlaylistRepositoryImpl) {
        bind<PlaylistRepository>()
      }
    }

  private val database: Database by inject()
  private val dao: PlaylistDao by inject()
  private val contentApi: ContentApi by inject()

  private val repository: PlaylistRepository by inject()

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
  private fun createPlaylistEntity(
    name: String,
    url: String,
    dateAdded: Long = DEFAULT_DATE_ADDED,
    id: Long = 0
  ): PlaylistEntity = PlaylistEntity(
    name = name,
    url = url,
    dateAdded = dateAdded,
    id = id
  )

  private fun createPlaylistDto(name: String, url: String): PlaylistDto = PlaylistDto(
    name = name,
    url = url
  )

  @Test
  fun countShouldReturnCorrectCount() {
    runTest(testDispatcher) {
      val playlists =
        listOf(
          createPlaylistEntity(
            name = "Playlist 1",
            url = "playlist1"
          ),
          createPlaylistEntity(
            name = "Playlist 2",
            url = "playlist2"
          ),
          createPlaylistEntity(
            name = "Playlist 3",
            url = "playlist3"
          )
        )
      dao.insertAll(playlists)

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
  fun getAllShouldReturnAllPlaylists() {
    runTest(testDispatcher) {
      val playlists =
        listOf(
          createPlaylistEntity(
            name = "Playlist 1",
            url = "playlist1"
          ),
          createPlaylistEntity(
            name = "Playlist 2",
            url = "playlist2"
          ),
          createPlaylistEntity(
            name = "Playlist 3",
            url = "playlist3"
          )
        )
      dao.insertAll(playlists)

      val result = repository.getAll().asSnapshot()

      assertThat(result).hasSize(3)
      assertThat(result.map { it.name }).containsExactly("Playlist 1", "Playlist 2", "Playlist 3")
    }
  }

  @Test
  fun getAllShouldReturnEmptyWhenNoPlaylists() {
    runTest(testDispatcher) {
      val result = repository.getAll().asSnapshot()

      assertThat(result).isEmpty()
    }
  }

  @Test
  fun searchShouldReturnMatchingPlaylists() {
    runTest(testDispatcher) {
      val playlists =
        listOf(
          createPlaylistEntity(
            name = "Rock Playlist",
            url = "rock_playlist"
          ),
          createPlaylistEntity(
            name = "Pop Playlist",
            url = "pop_playlist"
          ),
          createPlaylistEntity(
            name = "Another Rock Mix",
            url = "another_rock_mix"
          )
        )
      dao.insertAll(playlists)

      val result = repository.search("Rock").asSnapshot()

      assertThat(result).hasSize(2)
      assertThat(result.map { it.name }).containsExactly("Rock Playlist", "Another Rock Mix")
    }
  }

  @Test
  fun searchShouldReturnEmptyWhenNoMatches() {
    runTest(testDispatcher) {
      val playlists =
        listOf(
          createPlaylistEntity(
            name = "Rock Playlist",
            url = "rock_playlist"
          ),
          createPlaylistEntity(
            name = "Pop Playlist",
            url = "pop_playlist"
          )
        )
      dao.insertAll(playlists)

      val result = repository.search("Jazz").asSnapshot()

      assertThat(result).isEmpty()
    }
  }

  @Test
  fun getByIdShouldReturnPlaylistWhenExists() {
    runTest(testDispatcher) {
      val playlist =
        createPlaylistEntity(
          name = "Playlist 1",
          url = "playlist1"
        )
      dao.insertAll(listOf(playlist))
      val insertedPlaylist = dao.all().first()

      val result = repository.getById(insertedPlaylist.id)

      assertThat(result).isNotNull()
      assertThat(result!!.name).isEqualTo("Playlist 1")
      assertThat(result.url).isEqualTo("playlist1")
      assertThat(result.id).isEqualTo(insertedPlaylist.id)
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
  fun getRemoteShouldFetchAndStoreNewPlaylists() {
    runTest(testDispatcher) {
      val remotePlaylists =
        listOf(
          createPlaylistDto(
            name = "Playlist 1",
            url = "playlist1"
          ),
          createPlaylistDto(
            name = "Playlist 2",
            url = "playlist2"
          )
        )
      every { contentApi.getPlaylists(any()) } returns flowOf(remotePlaylists)

      repository.getRemote(null)

      val storedPlaylists = dao.all()
      assertThat(storedPlaylists).hasSize(2)
      assertThat(storedPlaylists.map { it.name }).containsExactly("Playlist 1", "Playlist 2")
    }
  }

  @Test
  fun getRemoteShouldUpdateExistingPlaylists() {
    runTest(testDispatcher) {
      val existingPlaylist =
        createPlaylistEntity(
          name = "Playlist 1 Old",
          url = "playlist1",
          dateAdded = OLDER_DATE_ADDED
        )
      dao.insertAll(listOf(existingPlaylist))

      val remotePlaylists =
        listOf(
          createPlaylistDto(
            name = "Playlist 1 New",
            url = "playlist1"
          )
        )
      every { contentApi.getPlaylists(any()) } returns flowOf(remotePlaylists)

      repository.getRemote(null)

      // Force a delay to ensure onCompletion is executed
      val updatedPlaylists = dao.all()

      // Since PlaylistRepositoryImpl uses REPLACE conflict strategy and removes previous entries in onCompletion,
      // we expect a new playlist with a new ID, not the same ID as before
      assertThat(updatedPlaylists).hasSize(1)
      assertThat(updatedPlaylists.first().name).isEqualTo("Playlist 1 New")
      assertThat(updatedPlaylists.first().url).isEqualTo("playlist1")
      assertThat(updatedPlaylists.first().dateAdded).isGreaterThan(OLDER_DATE_ADDED)
    }
  }

  @Test
  fun getRemoteShouldRemovePreviousEntries() {
    runTest(testDispatcher) {
      val oldPlaylist =
        createPlaylistEntity(
          name = "Old Playlist",
          url = "old_playlist",
          dateAdded = OLDER_DATE_ADDED
        )
      dao.insertAll(listOf(oldPlaylist))

      val remotePlaylists =
        listOf(
          createPlaylistDto(
            name = "New Playlist",
            url = "new_playlist"
          )
        )
      every { contentApi.getPlaylists(any()) } returns flowOf(remotePlaylists)

      repository.getRemote(null)

      val storedPlaylists = dao.all()
      assertThat(storedPlaylists).hasSize(1)
      assertThat(storedPlaylists.first().name).isEqualTo("New Playlist")
      assertThat(storedPlaylists.first().url).isEqualTo("new_playlist")
    }
  }

  @Test
  fun getRemoteShouldHandleProgressCallback() {
    runTest(testDispatcher) {
      val progress: Progress = mockk(relaxed = true)
      val remotePlaylists =
        listOf(
          createPlaylistDto(
            name = "Playlist 1",
            url = "playlist1"
          )
        )
      every { contentApi.getPlaylists(progress) } returns flowOf(remotePlaylists)

      repository.getRemote(progress)

      @Suppress("IgnoredReturnValue")
      verify { contentApi.getPlaylists(progress) }
    }
  }

  @Test
  fun getRemoteShouldHandleMixOfNewAndExistingPlaylists() {
    runTest(testDispatcher) {
      val existingPlaylists =
        listOf(
          createPlaylistEntity(
            name = "Playlist 1",
            url = "playlist1",
            dateAdded = OLDER_DATE_ADDED
          ),
          createPlaylistEntity(
            name = "Playlist 2",
            url = "playlist2",
            dateAdded = OLDER_DATE_ADDED
          )
        )
      dao.insertAll(existingPlaylists)

      val remotePlaylists =
        listOf(
          createPlaylistDto(
            name = "Playlist 1 Updated",
            url = "playlist1"
          ),
          createPlaylistDto(
            name = "Playlist 3",
            url = "playlist3"
          )
        )
      every { contentApi.getPlaylists(any()) } returns flowOf(remotePlaylists)

      repository.getRemote(null)

      val storedPlaylists = dao.all().sortedBy { it.name }

      // Since PlaylistRepositoryImpl uses REPLACE conflict strategy and removes previous entries in onCompletion,
      // we expect only the playlists from the remote source
      assertThat(storedPlaylists).hasSize(2)
      assertThat(
        storedPlaylists.map {
          it.name
        }
      ).containsExactly("Playlist 1 Updated", "Playlist 3")
      assertThat(storedPlaylists.map { it.url }).containsExactly("playlist1", "playlist3")
    }
  }

  @Test
  fun getBrowserItemsAtPathShouldReturnFoldersAndPlaylistsAtRoot() {
    runTest(testDispatcher) {
      // Given: playlists with mixed paths - some in folders, some at root
      val playlists = listOf(
        createPlaylistEntity(name = "MyMusic\\Tracks", url = "mymusic_tracks"),
        createPlaylistEntity(name = "MyMusic\\Other", url = "mymusic_other"),
        createPlaylistEntity(name = "Favorites", url = "favorites"),
        createPlaylistEntity(name = "Rock Hits", url = "rock_hits")
      )
      dao.insertAll(playlists)

      // When: getting browser items at root
      val result = repository.getBrowserItemsAtPath("").asSnapshot()

      // Then: should return MyMusic folder and root playlists
      assertThat(result).hasSize(3)

      // Folders should come first
      val folder = result.first { it.isFolder }
      assertThat(folder.name).isEqualTo("MyMusic")
      assertThat(folder.path).isEqualTo("MyMusic")

      // Playlists at root level
      val playlistNames = result.filter { it.isPlaylist }.map { it.name }
      assertThat(playlistNames).containsExactly("Favorites", "Rock Hits")
    }
  }

  @Test
  fun getBrowserItemsAtPathShouldReturnItemsInFolder() {
    runTest(testDispatcher) {
      // Given: playlists in a folder structure
      val playlists = listOf(
        createPlaylistEntity(name = "MyMusic\\Tracks", url = "mymusic_tracks"),
        createPlaylistEntity(name = "MyMusic\\SubFolder\\Deep", url = "deep_playlist"),
        createPlaylistEntity(name = "Favorites", url = "favorites")
      )
      dao.insertAll(playlists)

      // When: getting browser items at MyMusic folder
      val result = repository.getBrowserItemsAtPath("MyMusic").asSnapshot()

      // Then: should return SubFolder as folder and Tracks as playlist
      assertThat(result).hasSize(2)

      val folder = result.find { it.isFolder }
      assertThat(folder).isNotNull()
      assertThat(folder!!.name).isEqualTo("SubFolder")
      assertThat(folder.path).isEqualTo("MyMusic\\SubFolder")

      val playlist = result.find { it.isPlaylist }
      assertThat(playlist).isNotNull()
      assertThat(playlist!!.name).isEqualTo("Tracks")
      assertThat(playlist.path).isEqualTo("mymusic_tracks")
    }
  }

  @Test
  fun getBrowserItemsAtPathShouldGroupDuplicateFolders() {
    runTest(testDispatcher) {
      // Given: multiple playlists in the same folder
      val playlists = listOf(
        createPlaylistEntity(name = "Rock\\Playlist1", url = "rock1"),
        createPlaylistEntity(name = "Rock\\Playlist2", url = "rock2"),
        createPlaylistEntity(name = "Rock\\Playlist3", url = "rock3")
      )
      dao.insertAll(playlists)

      // When: getting browser items at root
      val result = repository.getBrowserItemsAtPath("").asSnapshot()

      // Then: should return only one Rock folder
      assertThat(result).hasSize(1)
      assertThat(result.first().isFolder).isTrue()
      assertThat(result.first().name).isEqualTo("Rock")
    }
  }

  @Test
  fun getBrowserItemsAtPathShouldKeepEveryFolderThoughTheyShareAnIdOfZero() {
    runTest(testDispatcher) {
      dao.insertAll(
        listOf(
          createPlaylistEntity(name = "Rock\\Playlist1", url = "rock1"),
          createPlaylistEntity(name = "Jazz\\Playlist2", url = "jazz1"),
          createPlaylistEntity(name = "Blues\\Playlist3", url = "blues1")
        )
      )

      val result = repository.getBrowserItemsAtPath("").asSnapshot()

      assertThat(result.map { it.name }).containsExactly("Blues", "Jazz", "Rock").inOrder()
    }
  }

  @Test
  fun getBrowserItemsAtPathShouldReturnEmptyWhenNoItemsAtPath() {
    runTest(testDispatcher) {
      // Given: playlists only at root
      val playlists = listOf(
        createPlaylistEntity(name = "Favorites", url = "favorites")
      )
      dao.insertAll(playlists)

      // When: getting browser items at non-existent folder
      val result = repository.getBrowserItemsAtPath("NonExistent").asSnapshot()

      // Then: should return empty list
      assertThat(result).isEmpty()
    }
  }

  @Test
  fun getBrowserItemsAtPathShouldHandleNestedFolders() {
    runTest(testDispatcher) {
      // Given: deeply nested folder structure
      val playlists = listOf(
        createPlaylistEntity(name = "A\\B\\C\\Deep", url = "deep")
      )
      dao.insertAll(playlists)

      // When: navigating through folders
      val rootItems = repository.getBrowserItemsAtPath("").asSnapshot()
      assertThat(rootItems).hasSize(1)
      assertThat(rootItems.first().name).isEqualTo("A")
      assertThat(rootItems.first().path).isEqualTo("A")

      val aItems = repository.getBrowserItemsAtPath("A").asSnapshot()
      assertThat(aItems).hasSize(1)
      assertThat(aItems.first().name).isEqualTo("B")
      assertThat(aItems.first().path).isEqualTo("A\\B")

      val bItems = repository.getBrowserItemsAtPath("A\\B").asSnapshot()
      assertThat(bItems).hasSize(1)
      assertThat(bItems.first().name).isEqualTo("C")
      assertThat(bItems.first().path).isEqualTo("A\\B\\C")

      val cItems = repository.getBrowserItemsAtPath("A\\B\\C").asSnapshot()
      assertThat(cItems).hasSize(1)
      assertThat(cItems.first().isPlaylist).isTrue()
      assertThat(cItems.first().name).isEqualTo("Deep")
    }
  }

  @Test
  fun getBrowserItemsAtPathShouldSortFoldersBeforePlaylists() {
    runTest(testDispatcher) {
      // Given: mix of folders and playlists that would sort differently
      val playlists = listOf(
        createPlaylistEntity(name = "Zebra\\Playlist", url = "zebra"),
        createPlaylistEntity(name = "Alpha", url = "alpha"),
        createPlaylistEntity(name = "Beta\\Playlist", url = "beta")
      )
      dao.insertAll(playlists)

      // When: getting browser items at root
      val result = repository.getBrowserItemsAtPath("").asSnapshot()

      // Then: folders should come before playlists
      assertThat(result).hasSize(3)
      assertThat(result[0].isFolder).isTrue()
      assertThat(result[1].isFolder).isTrue()
      assertThat(result[2].isPlaylist).isTrue()
    }
  }

  @Test
  fun getRemoteShouldPropagateExceptionWhenApiFails() {
    runTest(testDispatcher) {
      // Given: existing playlists in database
      val existingPlaylist = createPlaylistEntity(
        name = "Existing Playlist",
        url = "existing"
      )
      dao.insertAll(listOf(existingPlaylist))

      // And: API throws an exception
      every { contentApi.getPlaylists(any()) } returns flow {
        throw IOException("Network error")
      }

      // When & Then: exception should propagate
      var exceptionThrown = false
      try {
        repository.getRemote(null)
      } catch (e: IOException) {
        exceptionThrown = true
      }

      assertThat(exceptionThrown).isTrue()

      // Note: Due to onCompletion running regardless of exception status,
      // existing data is removed even when an exception occurs.
      // This documents the current behavior (which may be a bug to fix later).
      val storedPlaylists = dao.all()
      assertThat(storedPlaylists).isEmpty()
    }
  }

  @Test
  fun getRemoteShouldHandleEmptyApiResponse() {
    runTest(testDispatcher) {
      // Given: existing playlists in database
      val existingPlaylist = createPlaylistEntity(
        name = "Existing Playlist",
        url = "existing",
        dateAdded = OLDER_DATE_ADDED
      )
      dao.insertAll(listOf(existingPlaylist))

      // And: API returns empty list
      every { contentApi.getPlaylists(any()) } returns flowOf(emptyList())

      // When
      repository.getRemote(null)

      // Then: existing playlists should be removed (since they weren't in the remote response)
      val storedPlaylists = dao.all()
      assertThat(storedPlaylists).isEmpty()
    }
  }
}
