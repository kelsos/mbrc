package com.kelsos.mbrc.features.settings

import android.content.Context
import android.content.SharedPreferences
import android.content.res.Resources
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.paging.testing.asSnapshot
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import com.kelsos.mbrc.data.Database
import com.kelsos.mbrc.networking.discovery.RemoteServiceDiscovery
import com.kelsos.mbrc.utils.testDispatcher
import com.kelsos.mbrc.utils.testDispatcherModule
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.core.module.dsl.bind
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.module
import org.koin.test.KoinTest
import org.koin.test.inject
import org.mockito.ArgumentMatchers.anyInt
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`

@RunWith(AndroidJUnit4::class)
class ConnectionRepositoryTest : KoinTest {
  private val repository: ConnectionRepository by inject()

  private lateinit var db: Database
  private lateinit var connectionDao: ConnectionDao

  @get:Rule
  var instantTaskExecutorRule = InstantTaskExecutorRule()

  val testModule =
    module {
      includes(testDispatcherModule)

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
      singleOf(::ConnectionRepositoryImpl) { bind<ConnectionRepository>() }
      single { mockk<RemoteServiceDiscovery>() }
      single { connectionDao }
      single<Resources> {
        val resources = mock(Resources::class.java)
        `when`(resources.getString(anyInt())).thenReturn("preferences_key")
        resources
      }
    }

  @Before
  fun setUp() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    db =
      Room
        .inMemoryDatabaseBuilder(context, Database::class.java)
        .allowMainThreadQueries()
        .build()
    connectionDao = db.connectionDao()

    startKoin {
      modules(listOf(testModule))
    }
  }

  @After
  fun tearDown() {
    stopKoin()
    db.close()
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

  private data class Settings(
    val withId: List<ConnectionSettings>,
    val withoutId: List<ConnectionSettings>,
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

  private fun createSettings(lastOctet: Long): ConnectionSettings =
    ConnectionSettings(
      name = "Desktop PC $lastOctet",
      address = "198.51.100.$lastOctet",
      port = 3000,
      isDefault = lastOctet == 1L,
      id = 0,
    )
}
