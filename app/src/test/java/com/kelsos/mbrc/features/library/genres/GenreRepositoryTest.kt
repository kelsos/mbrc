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
class GenreRepositoryTest : KoinTest {
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
      single { get<Database>().genreDao() }
      singleOf(::GenreRepositoryImpl) {
        bind<GenreRepository>()
      }
    }

  private val database: Database by inject()
  private val dao: GenreDao by inject()
  private val api: ApiBase by inject()

  private val repository: GenreRepository by inject()

  @Before
  fun setUp() {
    startKoin { modules(listOf(testModule, testDispatcherModule)) }
  }

  @After
  fun tearDown() {
    database.close()
    stopKoin()
  }

  @Test
  fun countShouldReturnCorrectCount() {
    runTest(testDispatcher) {
      val genres =
        listOf(
          GenreEntity(genre = "Rock", dateAdded = 1000L),
          GenreEntity(genre = "Pop", dateAdded = 1000L),
          GenreEntity(genre = "Jazz", dateAdded = 1000L)
        )
      dao.insertAll(genres)

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
  fun getAllShouldReturnAllGenresSorted() {
    runTest(testDispatcher) {
      val genres =
        listOf(
          GenreEntity(genre = "Rock", dateAdded = 1000L),
          GenreEntity(genre = "Jazz", dateAdded = 1000L),
          GenreEntity(genre = "Pop", dateAdded = 1000L)
        )
      dao.insertAll(genres)

      val result = repository.getAll().asSnapshot()

      assertThat(result.map { it.genre }).containsExactly("Jazz", "Pop", "Rock").inOrder()
    }
  }

  @Test
  fun getAllShouldReturnEmptyWhenNoGenres() {
    runTest(testDispatcher) {
      val result = repository.getAll().asSnapshot()

      assertThat(result).isEmpty()
    }
  }

  @Test
  fun searchShouldReturnMatchingGenres() {
    runTest(testDispatcher) {
      val genres =
        listOf(
          GenreEntity(genre = "Rock", dateAdded = 1000L),
          GenreEntity(genre = "Pop Rock", dateAdded = 1000L),
          GenreEntity(genre = "Jazz", dateAdded = 1000L),
          GenreEntity(genre = "Hard Rock", dateAdded = 1000L)
        )
      dao.insertAll(genres)

      val result = repository.search("Rock").asSnapshot()

      assertThat(result.map { it.genre }).containsExactly("Hard Rock", "Pop Rock", "Rock").inOrder()
    }
  }

  @Test
  fun searchShouldReturnEmptyWhenNoMatches() {
    runTest(testDispatcher) {
      val genres =
        listOf(
          GenreEntity(genre = "Rock", dateAdded = 1000L),
          GenreEntity(genre = "Pop", dateAdded = 1000L)
        )
      dao.insertAll(genres)

      val result = repository.search("Classical").asSnapshot()

      assertThat(result).isEmpty()
    }
  }

  @Test
  fun searchShouldBeCaseInsensitive() {
    runTest(testDispatcher) {
      val genres =
        listOf(
          GenreEntity(genre = "Rock", dateAdded = 1000L),
          GenreEntity(genre = "JAZZ", dateAdded = 1000L),
          GenreEntity(genre = "pop", dateAdded = 1000L)
        )
      dao.insertAll(genres)

      val result = repository.search("rock").asSnapshot()

      assertThat(result.map { it.genre }).containsExactly("Rock")
    }
  }

  @Test
  fun getByIdShouldReturnGenreWhenExists() {
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
  fun getByIdShouldReturnNullWhenNotExists() {
    runTest(testDispatcher) {
      val result = repository.getById(999L)

      assertThat(result).isNull()
    }
  }

  @Test
  fun getRemoteShouldFetchAndStoreNewGenres() {
    runTest(testDispatcher) {
      val remoteGenres =
        listOf(
          GenreDto(genre = "Rock"),
          GenreDto(genre = "Pop")
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
  fun getRemoteShouldUpdateExistingGenres() {
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
  fun getRemoteShouldRemovePreviousEntries() {
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
  fun getRemoteShouldHandleProgressCallback() {
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
  fun getRemoteShouldHandleMixOfNewAndExistingGenres() {
    runTest(testDispatcher) {
      val existingGenres =
        listOf(
          GenreEntity(genre = "Rock", dateAdded = 500L),
          GenreEntity(genre = "Jazz", dateAdded = 500L)
        )
      dao.insertAll(existingGenres)
      val rockId = dao.genres().first { it.genre == "Rock" }.id

      val remoteGenres =
        listOf(
          GenreDto(genre = "Rock"),
          GenreDto(genre = "Pop")
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
