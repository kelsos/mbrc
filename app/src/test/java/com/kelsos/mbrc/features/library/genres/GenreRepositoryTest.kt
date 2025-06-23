@file:OptIn(ExperimentalCoroutinesApi::class)

package com.kelsos.mbrc.features.library.genres

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
class GenreRepositoryTest {
  private lateinit var database: Database
  private lateinit var repository: GenreRepository
  private lateinit var dao: GenreDao
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
    dao = database.genreDao()
    repository = GenreRepositoryImpl(api, dao, testDispatchers)
  }

  @After
  fun tearDown() {
    database.close()
  }

  @Test
  fun count_shouldReturnCorrectCount() {
    runTest(testDispatcher) {
      val genres =
        listOf(
          GenreEntity(genre = "Rock", dateAdded = 1000L),
          GenreEntity(genre = "Pop", dateAdded = 1000L),
          GenreEntity(genre = "Jazz", dateAdded = 1000L),
        )
      dao.insertAll(genres)

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
  fun getAll_shouldReturnAllGenresSorted() {
    runTest(testDispatcher) {
      val genres =
        listOf(
          GenreEntity(genre = "Rock", dateAdded = 1000L),
          GenreEntity(genre = "Jazz", dateAdded = 1000L),
          GenreEntity(genre = "Pop", dateAdded = 1000L),
        )
      dao.insertAll(genres)

      val result = repository.getAll().asSnapshot()

      assertThat(result.map { it.genre }).containsExactly("Jazz", "Pop", "Rock").inOrder()
    }
  }

  @Test
  fun getAll_shouldReturnEmptyWhenNoGenres() {
    runTest(testDispatcher) {
      val result = repository.getAll().asSnapshot()

      assertThat(result).isEmpty()
    }
  }

  @Test
  fun search_shouldReturnMatchingGenres() {
    runTest(testDispatcher) {
      val genres =
        listOf(
          GenreEntity(genre = "Rock", dateAdded = 1000L),
          GenreEntity(genre = "Pop Rock", dateAdded = 1000L),
          GenreEntity(genre = "Jazz", dateAdded = 1000L),
          GenreEntity(genre = "Hard Rock", dateAdded = 1000L),
        )
      dao.insertAll(genres)

      val result = repository.search("Rock").asSnapshot()

      assertThat(result.map { it.genre }).containsExactly("Hard Rock", "Pop Rock", "Rock").inOrder()
    }
  }

  @Test
  fun search_shouldReturnEmptyWhenNoMatches() {
    runTest(testDispatcher) {
      val genres =
        listOf(
          GenreEntity(genre = "Rock", dateAdded = 1000L),
          GenreEntity(genre = "Pop", dateAdded = 1000L),
        )
      dao.insertAll(genres)

      val result = repository.search("Classical").asSnapshot()

      assertThat(result).isEmpty()
    }
  }

  @Test
  fun search_shouldBeCaseInsensitive() {
    runTest(testDispatcher) {
      val genres =
        listOf(
          GenreEntity(genre = "Rock", dateAdded = 1000L),
          GenreEntity(genre = "JAZZ", dateAdded = 1000L),
          GenreEntity(genre = "pop", dateAdded = 1000L),
        )
      dao.insertAll(genres)

      val result = repository.search("rock").asSnapshot()

      assertThat(result.map { it.genre }).containsExactly("Rock")
    }
  }

  @Test
  fun getById_shouldReturnGenreWhenExists() {
    runTest(testDispatcher) {
      val genre = GenreEntity(genre = "Rock", dateAdded = 1000L)
      dao.insertAll(listOf(genre))
      val insertedGenre = dao.all().first()

      val result = repository.getById(insertedGenre.id!!)

      assertThat(result).isNotNull()
      assertThat(result!!.genre).isEqualTo("Rock")
      assertThat(result.id).isEqualTo(insertedGenre.id)
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
  fun getRemote_shouldFetchAndStoreNewGenres() {
    runTest(testDispatcher) {
      val remoteGenres =
        listOf(
          GenreDto(genre = "Rock"),
          GenreDto(genre = "Pop"),
        )
      coEvery {
        api.getAllPages(Protocol.LibraryBrowseGenres, GenreDto::class, any())
      } returns flowOf(remoteGenres)

      repository.getRemote(null)

      val storedGenres = dao.all()
      assertThat(storedGenres.map { it.genre }).containsExactly("Rock", "Pop")
    }
  }

  @Test
  fun getRemote_shouldUpdateExistingGenres() {
    runTest(testDispatcher) {
      val existingGenre = GenreEntity(genre = "Rock", dateAdded = 500L)
      dao.insertAll(listOf(existingGenre))
      val insertedId = dao.genres().first { it.genre == "Rock" }.id

      val remoteGenres = listOf(GenreDto(genre = "Rock"))
      coEvery {
        api.getAllPages(Protocol.LibraryBrowseGenres, GenreDto::class, any())
      } returns flowOf(remoteGenres)

      repository.getRemote(null)

      val updatedGenres = dao.all()
      assertThat(updatedGenres).hasSize(1)
      assertThat(updatedGenres.first().id).isEqualTo(insertedId)
      assertThat(updatedGenres.first().dateAdded).isGreaterThan(500L)
    }
  }

  @Test
  fun getRemote_shouldRemovePreviousEntries() {
    runTest(testDispatcher) {
      val oldGenre = GenreEntity(genre = "Old Genre", dateAdded = 500L)
      dao.insertAll(listOf(oldGenre))

      val remoteGenres = listOf(GenreDto(genre = "New Genre"))
      coEvery {
        api.getAllPages(Protocol.LibraryBrowseGenres, GenreDto::class, any())
      } returns flowOf(remoteGenres)

      repository.getRemote(null)

      val storedGenres = dao.all()
      assertThat(storedGenres).hasSize(1)
      assertThat(storedGenres.first().genre).isEqualTo("New Genre")
    }
  }

  @Test
  fun getRemote_shouldHandleProgressCallback() {
    runTest(testDispatcher) {
      val progress: Progress = mockk(relaxed = true)
      val remoteGenres = listOf(GenreDto(genre = "Rock"))
      coEvery {
        api.getAllPages(Protocol.LibraryBrowseGenres, GenreDto::class, progress)
      } returns flowOf(remoteGenres)

      repository.getRemote(progress)

      @Suppress("IgnoredReturnValue")
      verify { api.getAllPages(Protocol.LibraryBrowseGenres, GenreDto::class, progress) }
    }
  }

  @Test
  fun getRemote_shouldHandleMixOfNewAndExistingGenres() {
    runTest(testDispatcher) {
      val existingGenres =
        listOf(
          GenreEntity(genre = "Rock", dateAdded = 500L),
          GenreEntity(genre = "Jazz", dateAdded = 500L),
        )
      dao.insertAll(existingGenres)
      val rockId = dao.genres().first { it.genre == "Rock" }.id

      val remoteGenres =
        listOf(
          GenreDto(genre = "Rock"),
          GenreDto(genre = "Pop"),
        )
      coEvery {
        api.getAllPages(Protocol.LibraryBrowseGenres, GenreDto::class, any())
      } returns flowOf(remoteGenres)

      repository.getRemote(null)

      val storedGenres = dao.all().sortedBy { it.genre }
      assertThat(storedGenres).hasSize(2)
      assertThat(storedGenres.map { it.genre }).containsExactly("Pop", "Rock")
      assertThat(storedGenres.first { it.genre == "Rock" }.id).isEqualTo(rockId)
    }
  }
}
