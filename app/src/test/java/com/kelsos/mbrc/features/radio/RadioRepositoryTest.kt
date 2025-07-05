@file:OptIn(ExperimentalCoroutinesApi::class)

package com.kelsos.mbrc.features.radio

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
class RadioRepositoryTest : KoinTest {
  private val testModule =
    module {
      single<ApiBase> { mockk() }
      single {
        Room
          .inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            Database::class.java,
          ).allowMainThreadQueries()
          .build()
      }
      single { get<Database>().radioStationDao() }
      singleOf(::RadioRepositoryImpl) {
        bind<RadioRepository>()
      }
    }

  private val database: Database by inject()
  private val dao: RadioStationDao by inject()
  private val api: ApiBase by inject()

  private val repository: RadioRepository by inject()

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
      val radioStations =
        listOf(
          RadioStationEntity(name = "Radio 1", url = "http://radio1.com", dateAdded = 1000L),
          RadioStationEntity(name = "Radio 2", url = "http://radio2.com", dateAdded = 1000L),
          RadioStationEntity(name = "Radio 3", url = "http://radio3.com", dateAdded = 1000L),
        )
      dao.insertAll(radioStations)

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
  fun getAllShouldReturnAllRadioStationsSorted() {
    runTest(testDispatcher) {
      val radioStations =
        listOf(
          RadioStationEntity(name = "Z Radio", url = "http://zradio.com", dateAdded = 1000L),
          RadioStationEntity(name = "A Radio", url = "http://aradio.com", dateAdded = 1000L),
          RadioStationEntity(name = "M Radio", url = "http://mradio.com", dateAdded = 1000L),
        )
      dao.insertAll(radioStations)

      val result = repository.getAll().asSnapshot()

      assertThat(result).hasSize(3)
      assertThat(result.map { it.name }).containsExactly("Z Radio", "A Radio", "M Radio")
    }
  }

  @Test
  fun getAllShouldReturnEmptyWhenNoRadioStations() {
    runTest(testDispatcher) {
      val result = repository.getAll().asSnapshot()

      assertThat(result).isEmpty()
    }
  }

  @Test
  fun searchShouldReturnMatchingRadioStations() {
    runTest(testDispatcher) {
      val radioStations =
        listOf(
          RadioStationEntity(name = "Rock FM", url = "http://rockfm.com", dateAdded = 1000L),
          RadioStationEntity(name = "Pop Rock Radio", url = "http://poprock.com", dateAdded = 1000L),
          RadioStationEntity(name = "Jazz Station", url = "http://jazz.com", dateAdded = 1000L),
          RadioStationEntity(name = "Hard Rock", url = "http://hardrock.com", dateAdded = 1000L),
        )
      dao.insertAll(radioStations)

      val result = repository.search("Rock").asSnapshot()

      assertThat(result).hasSize(3)
      assertThat(result.map { it.name }).containsExactly("Rock FM", "Pop Rock Radio", "Hard Rock")
    }
  }

  @Test
  fun searchShouldReturnEmptyWhenNoMatches() {
    runTest(testDispatcher) {
      val radioStations =
        listOf(
          RadioStationEntity(name = "Rock FM", url = "http://rockfm.com", dateAdded = 1000L),
          RadioStationEntity(name = "Pop Station", url = "http://pop.com", dateAdded = 1000L),
        )
      dao.insertAll(radioStations)

      val result = repository.search("Classical").asSnapshot()

      assertThat(result).isEmpty()
    }
  }

  @Test
  fun searchShouldBeCaseInsensitive() {
    runTest(testDispatcher) {
      val radioStations =
        listOf(
          RadioStationEntity(name = "Rock FM", url = "http://rockfm.com", dateAdded = 1000L),
          RadioStationEntity(name = "JAZZ", url = "http://jazz.com", dateAdded = 1000L),
          RadioStationEntity(name = "pop", url = "http://pop.com", dateAdded = 1000L),
        )
      dao.insertAll(radioStations)

      val result = repository.search("rock").asSnapshot()

      assertThat(result.map { it.name }).containsExactly("Rock FM")
    }
  }

  @Test
  fun getByIdShouldReturnRadioStationWhenExists() {
    runTest(testDispatcher) {
      val radioStation = RadioStationEntity(name = "Radio 1", url = "http://radio1.com", dateAdded = 1000L)
      dao.insertAll(listOf(radioStation))
      val insertedRadioStation = dao.all().first()

      val result = repository.getById(insertedRadioStation.id!!)

      assertThat(result).isNotNull()
      assertThat(result!!.name).isEqualTo("Radio 1")
      assertThat(result.url).isEqualTo("http://radio1.com")
      assertThat(result.id).isEqualTo(insertedRadioStation.id)
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
  fun getRemoteShouldFetchAndStoreNewRadioStations() {
    runTest(testDispatcher) {
      val remoteRadioStations =
        listOf(
          RadioStationDto(name = "Radio 1", url = "http://radio1.com"),
          RadioStationDto(name = "Radio 2", url = "http://radio2.com"),
        )
      coEvery {
        api.getAllPages(Protocol.RadioStations, RadioStationDto::class, any())
      } returns flowOf(remoteRadioStations)

      repository.getRemote(null)

      val storedRadioStations = dao.all()
      assertThat(storedRadioStations.map { it.name }).containsExactly("Radio 1", "Radio 2")
      assertThat(storedRadioStations.map { it.url }).containsExactly("http://radio1.com", "http://radio2.com")
    }
  }

  @Test
  fun getRemoteShouldReplaceExistingRadioStations() {
    runTest(testDispatcher) {
      val existingRadioStation = RadioStationEntity(name = "Radio 1", url = "http://radio1.com", dateAdded = 500L)
      dao.insertAll(listOf(existingRadioStation))
      val insertedId = dao.all().first { it.name == "Radio 1" }.id

      val remoteRadioStations = listOf(RadioStationDto(name = "Radio 1", url = "http://radio1.com"))
      coEvery {
        api.getAllPages(Protocol.RadioStations, RadioStationDto::class, any())
      } returns flowOf(remoteRadioStations)

      repository.getRemote(null)

      val updatedRadioStations = dao.all()
      assertThat(updatedRadioStations).hasSize(1)
      assertThat(updatedRadioStations.first().name).isEqualTo("Radio 1")
      assertThat(updatedRadioStations.first().url).isEqualTo("http://radio1.com")
      assertThat(updatedRadioStations.first().dateAdded).isGreaterThan(500L)
      // The ID will be different since it's a new insert
      assertThat(updatedRadioStations.first().id).isNotEqualTo(insertedId)
    }
  }

  @Test
  fun getRemoteShouldRemovePreviousEntries() {
    runTest(testDispatcher) {
      val oldRadioStation = RadioStationEntity(name = "Old Radio", url = "http://oldradio.com", dateAdded = 500L)
      dao.insertAll(listOf(oldRadioStation))

      val remoteRadioStations = listOf(RadioStationDto(name = "New Radio", url = "http://newradio.com"))
      coEvery {
        api.getAllPages(Protocol.RadioStations, RadioStationDto::class, any())
      } returns flowOf(remoteRadioStations)

      repository.getRemote(null)

      val storedRadioStations = dao.all()
      assertThat(storedRadioStations).hasSize(1)
      assertThat(storedRadioStations.first().name).isEqualTo("New Radio")
      assertThat(storedRadioStations.first().url).isEqualTo("http://newradio.com")
    }
  }

  @Test
  fun getRemoteShouldHandleProgressCallback() {
    runTest(testDispatcher) {
      val progress: Progress = mockk(relaxed = true)
      val remoteRadioStations = listOf(RadioStationDto(name = "Radio 1", url = "http://radio1.com"))
      coEvery {
        api.getAllPages(Protocol.RadioStations, RadioStationDto::class, progress)
      } returns flowOf(remoteRadioStations)

      repository.getRemote(progress)

      @Suppress("IgnoredReturnValue")
      verify { api.getAllPages(Protocol.RadioStations, RadioStationDto::class, progress) }
    }
  }

  @Test
  fun getRemoteShouldHandleMixOfNewAndExistingRadioStations() {
    runTest(testDispatcher) {
      val existingRadioStations =
        listOf(
          RadioStationEntity(name = "Radio 1", url = "http://radio1.com", dateAdded = 500L),
          RadioStationEntity(name = "Radio 2", url = "http://radio2.com", dateAdded = 500L),
        )
      dao.insertAll(existingRadioStations)

      val remoteRadioStations =
        listOf(
          RadioStationDto(name = "Radio 1", url = "http://radio1.com"),
          RadioStationDto(name = "Radio 3", url = "http://radio3.com"),
        )
      coEvery {
        api.getAllPages(Protocol.RadioStations, RadioStationDto::class, any())
      } returns flowOf(remoteRadioStations)

      repository.getRemote(null)

      val storedRadioStations = dao.all().sortedBy { it.name }
      assertThat(storedRadioStations).hasSize(2)
      assertThat(storedRadioStations.map { it.name }).containsExactly("Radio 1", "Radio 3")
      // Radio 2 should be removed since it wasn't in the remote response
      // Radio 1 should have a new ID since it's re-inserted
      // Radio 3 should be newly added
    }
  }

  @Test
  fun getRemoteShouldHandleEmptyRemoteResponse() {
    runTest(testDispatcher) {
      val existingRadioStation = RadioStationEntity(name = "Radio 1", url = "http://radio1.com", dateAdded = 500L)
      dao.insertAll(listOf(existingRadioStation))

      coEvery {
        api.getAllPages(Protocol.RadioStations, RadioStationDto::class, any())
      } returns flowOf(emptyList())

      repository.getRemote(null)

      val storedRadioStations = dao.all()
      assertThat(storedRadioStations).isEmpty()
    }
  }

  @Test
  fun getRemoteShouldHandleRadioStationsWithEmptyNames() {
    runTest(testDispatcher) {
      val remoteRadioStations =
        listOf(
          RadioStationDto(name = "", url = "http://radio1.com"),
          RadioStationDto(name = "Radio 2", url = ""),
        )
      coEvery {
        api.getAllPages(Protocol.RadioStations, RadioStationDto::class, any())
      } returns flowOf(remoteRadioStations)

      repository.getRemote(null)

      val storedRadioStations = dao.all()
      assertThat(storedRadioStations).hasSize(2)
      assertThat(storedRadioStations.map { it.name }).containsExactly("", "Radio 2")
      assertThat(storedRadioStations.map { it.url }).containsExactly("http://radio1.com", "")
    }
  }

  @Test
  fun mappingFromEntityToRadioStationShouldHandleNullValues() {
    runTest(testDispatcher) {
      val radioStationWithNulls = RadioStationEntity(name = null, url = null, dateAdded = 1000L)
      dao.insertAll(listOf(radioStationWithNulls))
      val insertedRadioStation = dao.all().first()

      val result = repository.getById(insertedRadioStation.id!!)

      assertThat(result).isNotNull()
      assertThat(result!!.name).isEqualTo("")
      assertThat(result.url).isEqualTo("")
      assertThat(result.id).isEqualTo(insertedRadioStation.id)
    }
  }

  @Test
  fun searchShouldHandleSpecialCharacters() {
    runTest(testDispatcher) {
      val radioStations =
        listOf(
          RadioStationEntity(name = "Rock & Roll FM", url = "http://rockroll.com", dateAdded = 1000L),
          RadioStationEntity(name = "Jazz's Best", url = "http://jazz.com", dateAdded = 1000L),
          RadioStationEntity(name = "Pop 100%", url = "http://pop.com", dateAdded = 1000L),
        )
      dao.insertAll(radioStations)

      val result = repository.search("&").asSnapshot()

      assertThat(result.map { it.name }).containsExactly("Rock & Roll FM")
    }
  }

  @Test
  fun getAllShouldReturnRadioStationsWithCorrectMapping() {
    runTest(testDispatcher) {
      val radioStation = RadioStationEntity(name = "Test Radio", url = "http://test.com", dateAdded = 1000L)
      dao.insertAll(listOf(radioStation))

      val result = repository.getAll().asSnapshot()

      assertThat(result).hasSize(1)
      val mappedRadioStation = result.first()
      assertThat(mappedRadioStation.name).isEqualTo("Test Radio")
      assertThat(mappedRadioStation.url).isEqualTo("http://test.com")
      assertThat(mappedRadioStation.id).isGreaterThan(0)
    }
  }
}
