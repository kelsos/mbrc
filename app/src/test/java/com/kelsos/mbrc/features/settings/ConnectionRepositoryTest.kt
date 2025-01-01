package com.kelsos.mbrc.features.settings

import android.content.SharedPreferences
import android.content.res.Resources
import android.os.Build
import com.google.common.truth.Truth.assertThat
import com.kelsos.mbrc.TestApplication
import com.kelsos.mbrc.common.utilities.AppCoroutineDispatchers
import com.kelsos.mbrc.rules.DBFlowTestRule
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain
import org.junit.rules.TestRule
import org.junit.runner.RunWith
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.core.module.dsl.bind
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.module
import org.koin.test.KoinTest
import org.koin.test.inject
import org.mockito.ArgumentMatchers.anyInt
import org.mockito.ArgumentMatchers.anyLong
import org.mockito.ArgumentMatchers.anyString
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(
  application = TestApplication::class,
  sdk = [Build.VERSION_CODES.VANILLA_ICE_CREAM],
)
class ConnectionRepositoryTest : KoinTest {
  private val testDispatcher = StandardTestDispatcher()
  private val repository: ConnectionRepository by inject()

  @Rule
  fun chain(): TestRule = RuleChain.outerRule(DBFlowTestRule.create())

  val testModule =
    module {
      single {
        AppCoroutineDispatchers(
          testDispatcher,
          testDispatcher,
          testDispatcher,
          testDispatcher,
        )
      }

      single<SharedPreferences> {
        val defaultId = longArrayOf(-1)
        val preferences = mock(SharedPreferences::class.java)
        val editor = mock(SharedPreferences.Editor::class.java)
        `when`(preferences.edit()).thenReturn(editor)
        `when`(preferences.getLong(anyString(), anyLong())).thenAnswer { defaultId[0] }
        `when`(editor.putLong(anyString(), anyLong())).then {
          val o = it.arguments[1]
          defaultId[0] = java.lang.Long.parseLong(o.toString())
          editor
        }
        preferences
      }
      singleOf(::ConnectionRepositoryImpl) { bind<ConnectionRepository>() }
      single<Resources> {
        val resources = mock(Resources::class.java)
        `when`(resources.getString(anyInt())).thenReturn("preferences_key")
        resources
      }
    }

  @Before
  @Throws(Exception::class)
  fun setUp() {
    startKoin {
      modules(listOf(testModule))
    }
  }

  @After
  @Throws(Exception::class)
  fun tearDown() {
    stopKoin()
  }

  @Test
  fun addNewSettings() =
    runTest(testDispatcher) {
      val settings = createSettings("192.167.90.10")
      repository.save(settings)

      assertThat(repository.getDefault()).isEqualTo(settings)
      assertThat(settings.id).isEqualTo(1)
    }

  @Test
  fun addMultipleNewSettings() =
    runTest(testDispatcher) {
      val settings = createSettings("192.167.90.10")
      val settings1 = createSettings("192.167.90.11")
      val settings2 = createSettings("192.167.90.12")
      val settings3 = createSettings("192.167.90.12")
      repository.save(settings)
      repository.save(settings1)
      repository.save(settings2)
      repository.save(settings3)

      assertThat(repository.getDefault()).isEqualTo(settings)
      assertThat(settings.id).isEqualTo(1)
      assertThat(repository.count()).isEqualTo(3)
    }

  @Test
  fun addMultipleNewSettingsRemoveOne() =
    runTest(testDispatcher) {
      val settings = createSettings("192.167.90.10")
      val settings1 = createSettings("192.167.90.11")
      val settings2 = createSettings("192.167.90.12")
      val settings3 = createSettings("192.167.90.13")

      repository.save(settings)
      repository.save(settings1)
      repository.save(settings2)
      repository.save(settings3)

      assertThat(repository.getDefault()).isEqualTo(settings)
      assertThat(settings.id).isEqualTo(1)
      assertThat(repository.count()).isEqualTo(4)

      repository.delete(settings2)

      val settingsList = ArrayList<ConnectionSettings>()
      settingsList.add(settings)
      settingsList.add(settings1)
      settingsList.add(settings3)

      assertThat(repository.count()).isEqualTo(3)
      assertThat(repository.getAll()).containsExactlyElementsIn(settingsList)
    }

  @Test
  fun changeDefault() =
    runTest(testDispatcher) {
      val settings = createSettings("192.167.90.10")
      val settings1 = createSettings("192.167.90.11")

      repository.save(settings)
      repository.save(settings1)

      assertThat(repository.getDefault()).isEqualTo(settings)

      repository.setDefault(settings1)

      assertThat(repository.getDefault()).isEqualTo(settings1)
    }

  @Test
  fun deleteSingleDefault() =
    runTest(testDispatcher) {
      val settings = createSettings("192.167.90.10")
      repository.save(settings)

      assertThat(settings.id).isEqualTo(1)
      assertThat(repository.getDefault()).isEqualTo(settings)

      repository.delete(settings)

      assertThat(repository.count()).isEqualTo(0)
      assertThat(repository.getDefault()).isNull()
      assertThat(repository.defaultId).isEqualTo(-1)
    }

  @Test
  fun deleteFromMultipleDefaultFirst() =
    runTest(testDispatcher) {
      val settings = createSettings("192.167.90.10")
      val settings1 = createSettings("192.167.90.11")
      val settings2 = createSettings("192.167.90.12")
      val settings3 = createSettings("192.167.90.14")

      repository.save(settings)
      repository.save(settings1)
      repository.save(settings2)
      repository.save(settings3)

      assertThat(repository.count()).isEqualTo(4)

      assertThat(settings.id).isEqualTo(1)
      assertThat(repository.getDefault()).isEqualTo(settings)

      repository.delete(settings)

      assertThat(repository.count()).isEqualTo(3)
      assertThat(repository.getDefault()).isEqualTo(settings1)
      assertThat(repository.defaultId).isEqualTo(2)
    }

  @Test
  fun deleteFromMultipleDefaultSecond() =
    runTest(testDispatcher) {
      val settings = createSettings("192.167.90.10")
      val settings1 = createSettings("192.167.90.11")
      val settings2 = createSettings("192.167.90.12")
      val settings3 = createSettings("192.167.90.14")

      repository.save(settings)
      repository.save(settings1)
      repository.save(settings2)
      repository.save(settings3)

      assertThat(repository.count()).isEqualTo(4)

      assertThat(settings.id).isEqualTo(1)
      assertThat(repository.getDefault()).isEqualTo(settings)

      repository.setDefault(settings1)
      assertThat(repository.getDefault()).isEqualTo(settings1)

      repository.delete(settings1)

      assertThat(repository.count()).isEqualTo(3)
      assertThat(repository.getDefault()).isEqualTo(settings)
      assertThat(repository.defaultId).isEqualTo(1)
    }

  @Test
  fun deleteFromMultipleDefaultLast() =
    runTest(testDispatcher) {
      val settings = createSettings("192.167.90.10")
      val settings1 = createSettings("192.167.90.11")
      val settings2 = createSettings("192.167.90.12")
      val settings3 = createSettings("192.167.90.14")

      repository.save(settings)
      repository.save(settings1)
      repository.save(settings2)
      repository.save(settings3)

      assertThat(repository.count()).isEqualTo(4)

      assertThat(settings.id).isEqualTo(1)
      assertThat(repository.getDefault()).isEqualTo(settings)

      repository.setDefault(settings3)
      assertThat(repository.getDefault()).isEqualTo(settings3)

      repository.delete(settings3)

      assertThat(repository.count()).isEqualTo(3)
      assertThat(repository.getDefault()).isEqualTo(settings2)
      assertThat(repository.defaultId).isEqualTo(3)
    }

  @Test
  fun deleteFromMultipleNonDefault() =
    runTest(testDispatcher) {
      val settings = createSettings("192.167.90.10")
      val settings1 = createSettings("192.167.90.11")
      val settings2 = createSettings("192.167.90.12")
      val settings3 = createSettings("192.167.90.14")

      repository.save(settings)
      repository.save(settings1)
      repository.save(settings2)
      repository.save(settings3)

      assertThat(repository.count()).isEqualTo(4)

      assertThat(settings.id).isEqualTo(1)
      assertThat(repository.getDefault()).isEqualTo(settings)

      repository.setDefault(settings3)
      assertThat(repository.getDefault()).isEqualTo(settings3)

      repository.delete(settings1)

      assertThat(repository.count()).isEqualTo(3)
      assertThat(repository.getDefault()).isEqualTo(settings3)
      assertThat(repository.defaultId).isEqualTo(4)
    }

  @Test
  fun updateSettings() =
    runTest(testDispatcher) {
      val newPort = 6060
      val address = "192.167.90.10"
      val newAddress = "192.167.90.11"

      val settings = createSettings(address)
      repository.save(settings)

      assertThat(settings.id).isEqualTo(1)
      val defaultSettings = repository.getDefault()

      assertThat(defaultSettings).isEqualTo(settings)
      assertThat(defaultSettings!!.port).isEqualTo(3000)
      assertThat(defaultSettings.address).isEqualTo(address)

      settings.port = newPort

      repository.update(settings)

      assertThat(repository.getDefault()!!.port).isEqualTo(newPort)

      settings.address = newAddress
      repository.update(settings)

      assertThat(repository.getDefault()!!.address).isEqualTo(newAddress)
    }

  private fun createSettings(address: String): ConnectionSettings {
    val settings = ConnectionSettings()
    settings.name = "Desktop PC"
    settings.address = address
    settings.port = 3000
    return settings
  }
}
