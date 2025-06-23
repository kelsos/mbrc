@file:OptIn(ExperimentalCoroutinesApi::class)

package com.kelsos.mbrc.features.playlists

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
class PlaylistRepositoryTest {
  private lateinit var database: Database
  private lateinit var repository: PlaylistRepository
  private lateinit var dao: PlaylistDao
  private val api: ApiBase = mockk()

  // Constants for test data
  private companion object {
    const val DEFAULT_DATE_ADDED = 1000L
    const val OLDER_DATE_ADDED = 500L
  }

  @Before
  fun setUp() {
    database =
      Room
        .inMemoryDatabaseBuilder(
          ApplicationProvider.getApplicationContext(),
          Database::class.java,
        ).allowMainThreadQueries()
        .build()
    dao = database.playlistDao()
    repository = PlaylistRepositoryImpl(dao, api, testDispatchers)
  }

  @After
  fun tearDown() {
    database.close()
  }

  // Helper methods for creating test data
  private fun createPlaylistEntity(
    name: String,
    url: String,
    dateAdded: Long = DEFAULT_DATE_ADDED,
    id: Long? = null,
  ): PlaylistEntity =
    PlaylistEntity(
      name = name,
      url = url,
      dateAdded = dateAdded,
      id = id,
    )

  private fun createPlaylistDto(
    name: String,
    url: String,
  ): PlaylistDto =
    PlaylistDto(
      name = name,
      url = url,
    )

  @Test
  fun count_shouldReturnCorrectCount() {
    runTest(testDispatcher) {
      val playlists =
        listOf(
          createPlaylistEntity(
            name = "Playlist 1",
            url = "playlist1",
          ),
          createPlaylistEntity(
            name = "Playlist 2",
            url = "playlist2",
          ),
          createPlaylistEntity(
            name = "Playlist 3",
            url = "playlist3",
          ),
        )
      dao.insertAll(playlists)

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
  fun getAll_shouldReturnAllPlaylists() {
    runTest(testDispatcher) {
      val playlists =
        listOf(
          createPlaylistEntity(
            name = "Playlist 1",
            url = "playlist1",
          ),
          createPlaylistEntity(
            name = "Playlist 2",
            url = "playlist2",
          ),
          createPlaylistEntity(
            name = "Playlist 3",
            url = "playlist3",
          ),
        )
      dao.insertAll(playlists)

      val result = repository.getAll().asSnapshot()

      assertThat(result).hasSize(3)
      assertThat(result.map { it.name }).containsExactly("Playlist 1", "Playlist 2", "Playlist 3")
    }
  }

  @Test
  fun getAll_shouldReturnEmptyWhenNoPlaylists() {
    runTest(testDispatcher) {
      val result = repository.getAll().asSnapshot()

      assertThat(result).isEmpty()
    }
  }

  @Test
  fun search_shouldReturnMatchingPlaylists() {
    runTest(testDispatcher) {
      val playlists =
        listOf(
          createPlaylistEntity(
            name = "Rock Playlist",
            url = "rock_playlist",
          ),
          createPlaylistEntity(
            name = "Pop Playlist",
            url = "pop_playlist",
          ),
          createPlaylistEntity(
            name = "Another Rock Mix",
            url = "another_rock_mix",
          ),
        )
      dao.insertAll(playlists)

      val result = repository.search("Rock").asSnapshot()

      assertThat(result).hasSize(2)
      assertThat(result.map { it.name }).containsExactly("Rock Playlist", "Another Rock Mix")
    }
  }

  @Test
  fun search_shouldReturnEmptyWhenNoMatches() {
    runTest(testDispatcher) {
      val playlists =
        listOf(
          createPlaylistEntity(
            name = "Rock Playlist",
            url = "rock_playlist",
          ),
          createPlaylistEntity(
            name = "Pop Playlist",
            url = "pop_playlist",
          ),
        )
      dao.insertAll(playlists)

      val result = repository.search("Jazz").asSnapshot()

      assertThat(result).isEmpty()
    }
  }

  @Test
  fun getById_shouldReturnPlaylistWhenExists() {
    runTest(testDispatcher) {
      val playlist =
        createPlaylistEntity(
          name = "Playlist 1",
          url = "playlist1",
        )
      dao.insertAll(listOf(playlist))
      val insertedPlaylist = dao.all().first()

      val result = repository.getById(insertedPlaylist.id!!)

      assertThat(result).isNotNull()
      assertThat(result!!.name).isEqualTo("Playlist 1")
      assertThat(result.url).isEqualTo("playlist1")
      assertThat(result.id).isEqualTo(insertedPlaylist.id)
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
  fun getRemote_shouldFetchAndStoreNewPlaylists() {
    runTest(testDispatcher) {
      val remotePlaylists =
        listOf(
          createPlaylistDto(
            name = "Playlist 1",
            url = "playlist1",
          ),
          createPlaylistDto(
            name = "Playlist 2",
            url = "playlist2",
          ),
        )
      coEvery {
        api.getAllPages(Protocol.PlaylistList, PlaylistDto::class, any())
      } returns flowOf(remotePlaylists)

      repository.getRemote(null)

      val storedPlaylists = dao.all()
      assertThat(storedPlaylists).hasSize(2)
      assertThat(storedPlaylists.map { it.name }).containsExactly("Playlist 1", "Playlist 2")
    }
  }

  @Test
  fun getRemote_shouldUpdateExistingPlaylists() {
    runTest(testDispatcher) {
      val existingPlaylist =
        createPlaylistEntity(
          name = "Playlist 1 Old",
          url = "playlist1",
          dateAdded = OLDER_DATE_ADDED,
        )
      dao.insertAll(listOf(existingPlaylist))

      val remotePlaylists =
        listOf(
          createPlaylistDto(
            name = "Playlist 1 New",
            url = "playlist1",
          ),
        )
      coEvery {
        api.getAllPages(Protocol.PlaylistList, PlaylistDto::class, any())
      } returns flowOf(remotePlaylists)

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
  fun getRemote_shouldRemovePreviousEntries() {
    runTest(testDispatcher) {
      val oldPlaylist =
        createPlaylistEntity(
          name = "Old Playlist",
          url = "old_playlist",
          dateAdded = OLDER_DATE_ADDED,
        )
      dao.insertAll(listOf(oldPlaylist))

      val remotePlaylists =
        listOf(
          createPlaylistDto(
            name = "New Playlist",
            url = "new_playlist",
          ),
        )
      coEvery {
        api.getAllPages(Protocol.PlaylistList, PlaylistDto::class, any())
      } returns flowOf(remotePlaylists)

      repository.getRemote(null)

      val storedPlaylists = dao.all()
      assertThat(storedPlaylists).hasSize(1)
      assertThat(storedPlaylists.first().name).isEqualTo("New Playlist")
      assertThat(storedPlaylists.first().url).isEqualTo("new_playlist")
    }
  }

  @Test
  fun getRemote_shouldHandleProgressCallback() {
    runTest(testDispatcher) {
      val progress: Progress = mockk(relaxed = true)
      val remotePlaylists =
        listOf(
          createPlaylistDto(
            name = "Playlist 1",
            url = "playlist1",
          ),
        )
      coEvery {
        api.getAllPages(Protocol.PlaylistList, PlaylistDto::class, progress)
      } returns flowOf(remotePlaylists)

      repository.getRemote(progress)

      @Suppress("IgnoredReturnValue")
      verify { api.getAllPages(Protocol.PlaylistList, PlaylistDto::class, progress) }
    }
  }

  @Test
  fun getRemote_shouldHandleMixOfNewAndExistingPlaylists() {
    runTest(testDispatcher) {
      val existingPlaylists =
        listOf(
          createPlaylistEntity(
            name = "Playlist 1",
            url = "playlist1",
            dateAdded = OLDER_DATE_ADDED,
          ),
          createPlaylistEntity(
            name = "Playlist 2",
            url = "playlist2",
            dateAdded = OLDER_DATE_ADDED,
          ),
        )
      dao.insertAll(existingPlaylists)

      val remotePlaylists =
        listOf(
          createPlaylistDto(
            name = "Playlist 1 Updated",
            url = "playlist1",
          ),
          createPlaylistDto(
            name = "Playlist 3",
            url = "playlist3",
          ),
        )
      coEvery {
        api.getAllPages(Protocol.PlaylistList, PlaylistDto::class, any())
      } returns flowOf(remotePlaylists)

      repository.getRemote(null)

      val storedPlaylists = dao.all().sortedBy { it.name }

      // Since PlaylistRepositoryImpl uses REPLACE conflict strategy and removes previous entries in onCompletion,
      // we expect only the playlists from the remote source
      assertThat(storedPlaylists).hasSize(2)
      assertThat(storedPlaylists.map { it.name }).containsExactly("Playlist 1 Updated", "Playlist 3")
      assertThat(storedPlaylists.map { it.url }).containsExactly("playlist1", "playlist3")
    }
  }
}
