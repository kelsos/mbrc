package com.kelsos.mbrc.features.radio.repository

import android.content.Context
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import com.kelsos.mbrc.common.utilities.paged
import com.kelsos.mbrc.data.Database
import com.kelsos.mbrc.features.radio.RadioStationDto
import com.kelsos.mbrc.features.radio.data.RadioStationDao
import com.kelsos.mbrc.features.radio.domain.RadioStation
import com.kelsos.mbrc.networking.ApiBase
import com.kelsos.mbrc.networking.protocol.Protocol
import com.kelsos.mbrc.utils.TestData
import com.kelsos.mbrc.utils.TestData.mockApi
import com.kelsos.mbrc.utils.observeOnce
import com.kelsos.mbrc.utils.result
import com.kelsos.mbrc.utils.testDispatcherModule
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.dsl.module
import org.koin.experimental.builder.singleBy
import org.koin.test.KoinTest
import org.koin.test.inject
import java.net.SocketTimeoutException

@RunWith(AndroidJUnit4::class)
class RadioRepositoryTest : KoinTest {

  private val repository: RadioRepository by inject()

  private lateinit var db: Database
  private lateinit var dao: RadioStationDao
  private lateinit var apiBase: ApiBase

  @get:Rule
  val rule = InstantTaskExecutorRule()

  @Before
  fun setUp() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    db = TestData.createDB(context)
    dao = db.radioStationDao()
    apiBase = mockk()

    startKoin {
      modules(
        listOf(
          module {
            single { dao }
            singleBy<RadioRepository, RadioRepositoryImpl>()
            single { apiBase }
          },
          testDispatcherModule
        )
      )
    }
  }

  @After
  fun tearDown() {
    db.close()
    stopKoin()
  }

  @Test
  fun `sync is failure if there is an exception`() = runBlocking {
    coEvery {
      apiBase.getAllPages(
        Protocol.RadioStations,
        RadioStationDto::class
      )
    } throws SocketTimeoutException()

    assertThat(repository.getRemote().isLeft()).isTrue()
  }

  @Test
  fun `sync remote data and update the database`() = runBlocking {
    assertThat(repository.cacheIsEmpty())
    coEvery { apiBase.getAllPages(Protocol.RadioStations, RadioStationDto::class, any()) } answers {
      mockApi(2) {
        RadioStationDto(name = "Radio $it", url = "http://radio.statio/$it")
      }
    }

    assertThat(repository.getRemote().result()).isInstanceOf(Unit::class.java)
    assertThat(repository.count()).isEqualTo(2)
    repository.getAll().paged().observeOnce { result ->
      assertThat(result).hasSize(2)
    }
  }

  @Test
  fun `it should filter the stations when searching`() = runBlocking {
    coEvery { apiBase.getAllPages(Protocol.RadioStations, RadioStationDto::class, any()) } answers {
      mockApi(5, listOf(RadioStationDto(name = "Heavy Metal", url = "http://heavy.metal.ru"))) {
        RadioStationDto(name = "Radio $it", url = "http://radio.statio/$it")
      }
    }

    assertThat(repository.getRemote().result()).isInstanceOf(Unit::class.java)
    repository.search("Metal").paged().observeOnce {
      assertThat(it).hasSize(1)
      assertThat(it).containsExactly(
        RadioStation(
          name = "Heavy Metal",
          url = "http://heavy.metal.ru",
          id = 6
        )
      )
    }
  }
}
