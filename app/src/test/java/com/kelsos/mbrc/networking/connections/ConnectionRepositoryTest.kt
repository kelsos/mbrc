package com.kelsos.mbrc.networking.connections

import android.app.Application
import android.content.Context
import android.content.SharedPreferences
import android.content.res.Resources
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import com.kelsos.mbrc.data.Database
import com.kelsos.mbrc.networking.discovery.RemoteServiceDiscovery
import com.kelsos.mbrc.preferences.AppDataStore
import com.kelsos.mbrc.preferences.AppDataStoreImpl
import com.kelsos.mbrc.preferences.ClientInformationStore
import com.kelsos.mbrc.preferences.ClientInformationStoreImpl
import com.kelsos.mbrc.utils.observeOnce
import com.kelsos.mbrc.utils.testDispatcherModule
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
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

@RunWith(AndroidJUnit4::class)
class ConnectionRepositoryTest : KoinTest {

  private val repository: ConnectionRepository by inject()

  private lateinit var db: Database
  private lateinit var connectionDao: ConnectionDao

  @get:Rule
  val rule = InstantTaskExecutorRule()

  @Before
  fun setUp() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    db = Room.inMemoryDatabaseBuilder(context, Database::class.java)
      .allowMainThreadQueries()
      .build()
    connectionDao = db.connectionDao()

    startKoin {
      modules(listOf(getTestModule(), testDispatcherModule))
    }
  }

  @After
  fun tearDown() {
    stopKoin()
    db.close()
  }

  @Test
  fun addNewSettings() {

    val settings = createSettings("192.167.90.10")

    runBlocking {
      repository.save(settings)
    }

    assertThat(runBlocking { repository.getDefault() }).isEqualTo(settings)
    assertThat(settings.id).isEqualTo(1)
  }

  @Test
  fun addMultipleNewSettings() {
    val settings = createSettings("192.167.90.10")
    val settings1 = createSettings("192.167.90.11")
    val settings2 = createSettings("192.167.90.12")
    val settings3 = createSettings("192.167.90.12")

    runBlocking {
      repository.save(settings)
      repository.save(settings1)
      repository.save(settings2)
      repository.save(settings3)
    }

    assertThat(runBlocking { repository.getDefault() }).isEqualTo(settings)
    assertThat(settings.id).isEqualTo(1)
    val count = runBlocking { repository.count() }
    assertThat(count).isEqualTo(3)
  }

  @Test
  fun addMultipleNewSettingsRemoveOne() {
    val settings = createSettings("192.167.90.10")
    val settings1 = createSettings("192.167.90.11")
    val settings2 = createSettings("192.167.90.12")
    val settings3 = createSettings("192.167.90.13")

    runBlocking {
      repository.save(settings)
      repository.save(settings1)
      repository.save(settings2)
      repository.save(settings3)
    }

    assertThat(runBlocking { repository.getDefault() }).isEqualTo(settings)
    assertThat(settings.id).isEqualTo(1)
    var count = runBlocking { repository.count() }
    assertThat(count).isEqualTo(4)

    runBlocking {
      repository.delete(settings2)
    }

    val settingsList = ArrayList<ConnectionSettingsEntity>()
    settingsList.add(settings)
    settingsList.add(settings1)
    settingsList.add(settings3)

    count = runBlocking { repository.count() }
    assertThat(count).isEqualTo(3)

    repository.getAll().observeOnce {
      assertThat(it).containsExactlyElementsIn(settingsList)
    }
  }

  @Test
  fun changeDefault() {

    val settings = createSettings("192.167.90.10")
    val settings1 = createSettings("192.167.90.11")

    runBlocking {
      repository.save(settings)
      repository.save(settings1)
    }

    assertThat(runBlocking { repository.getDefault() }).isEqualTo(settings)
    runBlocking { repository.setDefaultConnectionId(settings1.id) }
    assertThat(runBlocking { repository.getDefault() }).isEqualTo(settings1)
  }

  @Test
  fun deleteSingleDefault() {

    val settings = createSettings("192.167.90.10")
    runBlocking {
      repository.save(settings)
    }

    assertThat(settings.id).isEqualTo(1)
    assertThat(runBlocking { repository.getDefault() }).isEqualTo(settings)

    runBlocking {
      repository.delete(settings)
    }

    val count = runBlocking { repository.count() }
    assertThat(count).isEqualTo(0)
    assertThat(runBlocking { repository.getDefault() }).isNull()
  }

  @Test
  fun deleteFromMultipleDefaultFirst() {

    val settings = createSettings("192.167.90.10")
    val settings1 = createSettings("192.167.90.11")
    val settings2 = createSettings("192.167.90.12")
    val settings3 = createSettings("192.167.90.14")

    runBlocking {
      repository.save(settings)
      repository.save(settings1)
      repository.save(settings2)
      repository.save(settings3)
    }

    var count = runBlocking { repository.count() }
    assertThat(count).isEqualTo(4)

    assertThat(settings.id).isEqualTo(1)
    assertThat(runBlocking { repository.getDefault() }).isEqualTo(settings)

    runBlocking {
      repository.delete(settings)
    }

    count = runBlocking { repository.count() }
    assertThat(count).isEqualTo(3)
    assertThat(runBlocking { repository.getDefault() }).isEqualTo(settings1)
  }

  @Test
  fun deleteFromMultipleDefaultSecond() {

    val settings = createSettings("192.167.90.10")
    val settings1 = createSettings("192.167.90.11")
    val settings2 = createSettings("192.167.90.12")
    val settings3 = createSettings("192.167.90.14")

    runBlocking {
      repository.save(settings)
      repository.save(settings1)
      repository.save(settings2)
      repository.save(settings3)
    }

    var count = runBlocking { repository.count() }
    assertThat(count).isEqualTo(4)

    assertThat(settings.id).isEqualTo(1)
    assertThat(runBlocking { repository.getDefault() }).isEqualTo(settings)

    runBlocking { repository.setDefaultConnectionId(settings1.id) }
    assertThat(runBlocking { repository.getDefault() }).isEqualTo(settings1)

    runBlocking {
      repository.delete(settings1)
    }

    count = runBlocking { repository.count() }
    assertThat(count).isEqualTo(3)
    assertThat(runBlocking { repository.getDefault() }).isEqualTo(settings)
  }

  @Test
  fun deleteFromMultipleDefaultLast() {

    val settings = createSettings("192.167.90.10")
    val settings1 = createSettings("192.167.90.11")
    val settings2 = createSettings("192.167.90.12")
    val settings3 = createSettings("192.167.90.14")

    runBlocking {
      repository.save(settings)
      repository.save(settings1)
      repository.save(settings2)
      repository.save(settings3)
    }

    var count = runBlocking { repository.count() }
    assertThat(count).isEqualTo(4)

    assertThat(settings.id).isEqualTo(1)
    assertThat(runBlocking { repository.getDefault() }).isEqualTo(settings)

    runBlocking { repository.setDefaultConnectionId(settings3.id) }
    assertThat(runBlocking { repository.getDefault() }).isEqualTo(settings3)

    runBlocking {
      repository.delete(settings3)
    }

    count = runBlocking { repository.count() }
    assertThat(count).isEqualTo(3)
    assertThat(runBlocking { repository.getDefault() }).isEqualTo(settings2)
  }

  @Test
  fun deleteFromMultipleNonDefault() {

    val settings = createSettings("192.167.90.10")
    val settings1 = createSettings("192.167.90.11")
    val settings2 = createSettings("192.167.90.12")
    val settings3 = createSettings("192.167.90.14")

    runBlocking {
      repository.save(settings)
      repository.save(settings1)
      repository.save(settings2)
      repository.save(settings3)
    }

    var count = runBlocking { repository.count() }
    assertThat(count).isEqualTo(4)

    assertThat(settings.id).isEqualTo(1)
    assertThat(runBlocking { repository.getDefault() }).isEqualTo(settings)

    runBlocking { repository.setDefaultConnectionId(settings3.id) }
    assertThat(runBlocking { repository.getDefault() }).isEqualTo(settings3)

    runBlocking {
      repository.delete(settings1)
    }

    count = runBlocking { repository.count() }
    assertThat(count).isEqualTo(3)
    assertThat(runBlocking { repository.getDefault() }).isEqualTo(settings3)
  }

  @Test
  fun updateSettings() {
    val newPort = 6060
    val address = "192.167.90.10"
    val newAddress = "192.167.90.11"

    val settings = createSettings(address)

    runBlocking {
      repository.save(settings)
    }

    assertThat(settings.id).isEqualTo(1)
    val defaultSettings = runBlocking { repository.getDefault() }

    assertThat(defaultSettings).isEqualTo(settings)
    assertThat(defaultSettings!!.port).isEqualTo(3000)
    assertThat(defaultSettings.address).isEqualTo(address)

    settings.port = newPort

    runBlocking {
      repository.save(settings)
    }

    assertThat(runBlocking { repository.getDefault() }!!.port).isEqualTo(newPort)

    settings.address = newAddress

    runBlocking {
      repository.save(settings)
    }

    assertThat(runBlocking { repository.getDefault() }!!.address).isEqualTo(newAddress)
  }

  private fun createSettings(address: String): ConnectionSettingsEntity {
    val settings = ConnectionSettingsEntity()
    settings.name = "Desktop PC"
    settings.address = address
    settings.port = 3000
    return settings
  }

  private fun getTestModule() = module {

    single {
      val slot = slot<Long>()
      val preferences = mockk<SharedPreferences>()
      val editor = mockk<SharedPreferences.Editor>()
      every { preferences.edit() } returns editor
      every { preferences.getLong(any(), any()) } answers { slot.captured }
      every { editor.putLong(any(), capture(slot)) } returns editor
      preferences
    }

    single { mockk<RemoteServiceDiscovery>() }

    singleBy<ConnectionRepository, ConnectionRepositoryImpl>()
    single {
      val resources = mockk<Resources>()
      every { resources.getString(any()) } returns "preferences_key"
      resources
    }
    single { connectionDao }
    single<Application> { ApplicationProvider.getApplicationContext() }
    singleBy<ClientInformationStore, ClientInformationStoreImpl>()
    singleBy<AppDataStore, AppDataStoreImpl>()
  }
}
