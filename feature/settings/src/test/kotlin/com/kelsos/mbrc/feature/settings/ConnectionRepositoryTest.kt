package com.kelsos.mbrc.feature.settings

import android.content.Context
import android.content.SharedPreferences
import android.content.res.Resources
import androidx.paging.testing.asSnapshot
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import com.google.common.truth.Truth.assertWithMessage
import com.kelsos.mbrc.core.common.data.ConnectionSettings
import com.kelsos.mbrc.core.common.test.testDispatcher
import com.kelsos.mbrc.core.common.test.testDispatcherModule
import com.kelsos.mbrc.core.data.Database
import com.kelsos.mbrc.core.data.settings.ConnectionDao
import com.kelsos.mbrc.core.networking.discovery.DiscoveryStop
import com.kelsos.mbrc.core.networking.discovery.RemoteServiceDiscovery
import com.kelsos.mbrc.feature.settings.domain.ConnectionRepository
import com.kelsos.mbrc.feature.settings.domain.ConnectionRepositoryImpl
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
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
class ConnectionRepositoryTest : KoinTest {
  private val repository: ConnectionRepository by inject()

  private val db: Database by inject()

  private val discovery: RemoteServiceDiscovery by inject()

  private val testModule =
    module {
      single<SharedPreferences> {
        val defaultId = longArrayOf(-1)
        val preferences = mockk<SharedPreferences>(relaxed = true)
        val editor = mockk<SharedPreferences.Editor>(relaxed = true)

        every { preferences.edit() } returns editor
        every { preferences.getLong(any(), any()) } answers { defaultId[0] }

        every { editor.putLong(any(), any()) } answers {
          val o = secondArg<Long>()
          defaultId[0] = o
          editor
        }

        preferences
      }
      single<Database> {
        val context = ApplicationProvider.getApplicationContext<Context>()
        Room
          .inMemoryDatabaseBuilder(context, Database::class.java)
          .allowMainThreadQueries()
          .build()
      }
      singleOf(::ConnectionRepositoryImpl) { bind<ConnectionRepository>() }
      single { mockk<RemoteServiceDiscovery>() }
      single<ConnectionDao> { get<Database>().connectionDao() }
      single<Resources> {
        val resources = mockk<Resources>()
        every { resources.getString(any()) } returns "preferences_key"
        resources
      }
    }

  @Before
  fun setUp() {
    startKoin { modules(listOf(testModule, testDispatcherModule)) }
  }

  @After
  fun tearDown() {
    db.close()
    stopKoin()
  }

  @Test
  fun addNewSettings() {
    runTest(testDispatcher) {
      val (withId, withoutId) = generateSettings(1)

      repository.save(withoutId[0])

      val defaultSettings = repository.getDefault()
      assertThat(defaultSettings).isEqualTo(withId[0])
    }
  }

  @Test
  fun addMultipleNewSettings() {
    runTest(testDispatcher) {
      val (withId, withoutId) = generateSettings(3)

      for (setting in withoutId) {
        repository.save(setting)
      }

      assertThat(repository.getDefault()).isEqualTo(withId[0])
      assertThat(repository.count()).isEqualTo(3)
    }
  }

  @Test
  fun addMultipleNewSettingsRemoveOne() {
    runTest(testDispatcher) {
      val (withId, withoutId) = generateSettings(4)

      for (setting in withoutId) {
        repository.save(setting)
      }

      assertThat(repository.getDefault()).isEqualTo(withId[0])
      assertThat(repository.count()).isEqualTo(4)

      repository.delete(withId[2])

      val remaining = withId.minus(withId[2])
      assertThat(repository.count()).isEqualTo(3)
      val snapshot = repository.getAll().asSnapshot()
      assertThat(snapshot.toList()).containsExactlyElementsIn(remaining)
    }
  }

  @Test
  fun changeDefault() {
    runTest(testDispatcher) {
      val (withId, withoutId) = generateSettings(2)

      for (setting in withoutId) {
        repository.save(setting)
      }

      assertThat(repository.getDefault()).isEqualTo(withId[0])
      repository.setDefault(withId[1])
      assertThat(repository.getDefault()).isEqualTo(withId[1].copy(isDefault = true))
    }
  }

  @Test
  fun deleteSingleDefault() {
    runTest(testDispatcher) {
      val (withId, withoutId) = generateSettings(1)

      repository.save(withoutId[0])
      assertThat(repository.getDefault()).isEqualTo(withId[0])

      repository.delete(withId[0])

      assertThat(repository.count()).isEqualTo(0)
      assertThat(repository.getDefault()).isNull()
    }
  }

  @Test
  fun deleteFromMultipleDefaultFirst() {
    runTest(testDispatcher) {
      val (withId, withoutId) = generateSettings(4)

      for (setting in withoutId) {
        repository.save(setting)
      }

      assertThat(repository.count()).isEqualTo(4)
      assertThat(repository.getDefault()).isEqualTo(withId[0])
      repository.delete(withId[0])
      assertThat(repository.count()).isEqualTo(3)
      assertThat(repository.getDefault()).isEqualTo(withId[1].copy(isDefault = true))
    }
  }

  @Test
  fun deleteFromMultipleDefaultSecond() {
    runTest(testDispatcher) {
      val (withId, withoutId) = generateSettings(4)

      for (setting in withoutId) {
        repository.save(setting)
      }

      assertThat(repository.count()).isEqualTo(4)
      assertThat(repository.getDefault()).isEqualTo(withId[0])

      repository.setDefault(withId[1])
      assertThat(repository.getDefault()).isEqualTo(withId[1].copy(isDefault = true))

      repository.delete(withId[1])

      assertThat(repository.count()).isEqualTo(3)
      assertThat(repository.getDefault()).isEqualTo(withId[0])
    }
  }

  @Test
  fun deleteFromMultipleDefaultLast() {
    runTest(testDispatcher) {
      val (withId, withoutId) = generateSettings(4)

      for (setting in withoutId) {
        repository.save(setting)
      }

      assertThat(repository.count()).isEqualTo(4)
      assertThat(repository.getDefault()).isEqualTo(withId[0])

      repository.setDefault(withId[3])
      assertThat(repository.getDefault()).isEqualTo(withId[3].copy(isDefault = true))

      repository.delete(withId[3])

      assertThat(repository.count()).isEqualTo(3)
      assertThat(repository.getDefault()).isEqualTo(withId[2].copy(isDefault = true))
    }
  }

  @Test
  fun deleteFromMultipleNonDefault() {
    runTest(testDispatcher) {
      val (withId, withoutId) = generateSettings(4)

      for (setting in withoutId) {
        repository.save(setting)
      }

      assertThat(repository.count()).isEqualTo(4)
      assertThat(repository.getDefault()).isEqualTo(withId[0])

      repository.setDefault(withId[3])
      assertThat(repository.getDefault()).isEqualTo(withId[3].copy(isDefault = true))

      repository.delete(withId[2])

      assertThat(repository.count()).isEqualTo(3)
      assertThat(repository.getDefault()).isEqualTo(withId[3].copy(isDefault = true))
    }
  }

  @Test
  fun updateSettings() {
    runTest(testDispatcher) {
      val newPort = 6060
      val newAddress = "192.167.90.11"

      val (withId, withoutId) = generateSettings(4)

      repository.save(withoutId[0])
      val defaultSettings = repository.getDefault()

      assertThat(defaultSettings).isEqualTo(withId[0])
      assertThat(defaultSettings?.port).isEqualTo(3000)
      assertThat(defaultSettings?.address).isEqualTo(withoutId[0].address)

      repository.save(withId[0].copy(port = newPort))

      assertThat(repository.getDefault()?.port).isEqualTo(newPort)
      repository.save(withId[0].copy(address = newAddress))

      assertThat(repository.getDefault()?.address).isEqualTo(newAddress)
    }
  }

  @Test
  fun discoveredHostsAreSaved() {
    runTest(testDispatcher) {
      val (_, withoutId) = generateSettings(2)
      coEvery { discovery.discover() } returns DiscoveryStop.Complete(withoutId)

      repository.discover()

      assertThat(repository.count()).isEqualTo(2)
      val snapshot = repository.getAll().asSnapshot()
      assertThat(snapshot.map { it.address })
        .containsExactlyElementsIn(withoutId.map { it.address })
    }
  }

  @Test
  fun discoveringAKnownHostDoesNotDuplicateIt() {
    runTest(testDispatcher) {
      val (_, withoutId) = generateSettings(2)
      repository.save(withoutId[0])
      coEvery { discovery.discover() } returns DiscoveryStop.Complete(withoutId)

      repository.discover()

      assertWithMessage("the already saved host should not be stored twice")
        .that(repository.count())
        .isEqualTo(2)
    }
  }

  @Test
  fun rescanningDoesNotGrowTheStoredHosts() {
    runTest(testDispatcher) {
      val (_, withoutId) = generateSettings(3)
      coEvery { discovery.discover() } returns DiscoveryStop.Complete(withoutId)

      repository.discover()
      repository.discover()

      assertThat(repository.count()).isEqualTo(3)
    }
  }

  @Test
  fun anUnsuccessfulDiscoveryIsReportedUnchanged() {
    runTest(testDispatcher) {
      coEvery { discovery.discover() } returns DiscoveryStop.NotFound

      val result = repository.discover()

      assertThat(result).isEqualTo(DiscoveryStop.NotFound)
      assertThat(repository.count()).isEqualTo(0)
    }
  }

  private data class Settings(
    val withId: List<ConnectionSettings>,
    val withoutId: List<ConnectionSettings>
  )

  private fun generateSettings(count: Long): Settings {
    val withoutId = mutableListOf<ConnectionSettings>()
    val withId = mutableListOf<ConnectionSettings>()
    for (lastOctet in 1..count) {
      val setting = createSettings(lastOctet)
      withoutId.add(setting)
      withId.add(setting.copy(id = lastOctet))
    }
    return Settings(withId, withoutId)
  }

  private fun createSettings(lastOctet: Long): ConnectionSettings = ConnectionSettings(
    name = "Desktop PC $lastOctet",
    address = "198.51.100.$lastOctet",
    port = 3000,
    isDefault = lastOctet == 1L,
    id = 0
  )
}
