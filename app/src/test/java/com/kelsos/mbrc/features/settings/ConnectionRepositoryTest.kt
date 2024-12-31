package com.kelsos.mbrc.features.settings

import android.annotation.SuppressLint
import android.content.SharedPreferences
import android.content.res.Resources
import android.os.Build
import androidx.test.platform.app.InstrumentationRegistry
import com.google.common.truth.Truth
import com.kelsos.mbrc.TestApplication
import com.kelsos.mbrc.common.utilities.AppCoroutineDispatchers
import com.kelsos.mbrc.rules.DBFlowTestRule
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain
import org.junit.rules.TestRule
import org.junit.runner.RunWith
import org.mockito.ArgumentMatchers
import org.mockito.Mockito
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import toothpick.Toothpick
import toothpick.config.Module
import toothpick.testing.ToothPickRule

@RunWith(RobolectricTestRunner::class)
@Config(
  application = TestApplication::class,
  sdk = [Build.VERSION_CODES.VANILLA_ICE_CREAM],
)
class ConnectionRepositoryTest {
  private val toothPickRule = ToothPickRule(this)
  private val testDispatcher = StandardTestDispatcher()

  @Rule
  fun chain(): TestRule = RuleChain.outerRule(toothPickRule).around(DBFlowTestRule.Companion.create())

  @Test
  fun addNewSettings() = runTest(testDispatcher) {
    val repository = repository
    val settings = createSettings("192.167.90.10")
    repository.save(settings)

    Truth.assertThat(repository.getDefault()).isEqualTo(settings)
    Truth.assertThat(settings.id).isEqualTo(1)
  }

  @Test
  fun addMultipleNewSettings() = runTest(testDispatcher) {
    val repository = repository
    val settings = createSettings("192.167.90.10")
    val settings1 = createSettings("192.167.90.11")
    val settings2 = createSettings("192.167.90.12")
    val settings3 = createSettings("192.167.90.12")
    repository.save(settings)
    repository.save(settings1)
    repository.save(settings2)
    repository.save(settings3)

    Truth.assertThat(repository.getDefault()).isEqualTo(settings)
    Truth.assertThat(settings.id).isEqualTo(1)
    Truth.assertThat(repository.count()).isEqualTo(3)
  }

  private val repository: ConnectionRepository
    get() {
      val scope = Toothpick.openScope(InstrumentationRegistry.getInstrumentation().targetContext)
      scope.installModules(
        TestModule(),
        object : Module() {
          init {
            bind(AppCoroutineDispatchers::class.java).toInstance(
              AppCoroutineDispatchers(
                testDispatcher,
                testDispatcher,
                testDispatcher,
              ),
            )
          }
        },
      )
      return scope.getInstance(ConnectionRepository::class.java)
    }

  @Test
  fun addMultipleNewSettingsRemoveOne() = runTest(testDispatcher) {
    val repository = repository
    val settings = createSettings("192.167.90.10")
    val settings1 = createSettings("192.167.90.11")
    val settings2 = createSettings("192.167.90.12")
    val settings3 = createSettings("192.167.90.13")

    repository.save(settings)
    repository.save(settings1)
    repository.save(settings2)
    repository.save(settings3)

    Truth.assertThat(repository.getDefault()).isEqualTo(settings)
    Truth.assertThat(settings.id).isEqualTo(1)
    Truth.assertThat(repository.count()).isEqualTo(4)

    repository.delete(settings2)

    val settingsList = ArrayList<ConnectionSettings>()
    settingsList.add(settings)
    settingsList.add(settings1)
    settingsList.add(settings3)

    Truth.assertThat(repository.count()).isEqualTo(3)
    Truth.assertThat(repository.getAll()).containsExactlyElementsIn(settingsList)
  }

  @Test
  fun changeDefault() = runTest(testDispatcher) {
    val repository = repository

    val settings = createSettings("192.167.90.10")
    val settings1 = createSettings("192.167.90.11")

    repository.save(settings)
    repository.save(settings1)

    Truth.assertThat(repository.getDefault()).isEqualTo(settings)

    repository.setDefault(settings1)

    Truth.assertThat(repository.getDefault()).isEqualTo(settings1)
  }

  @Test
  fun deleteSingleDefault() = runTest(testDispatcher) {
    val repository = repository

    val settings = createSettings("192.167.90.10")
    repository.save(settings)

    Truth.assertThat(settings.id).isEqualTo(1)
    Truth.assertThat(repository.getDefault()).isEqualTo(settings)

    repository.delete(settings)

    Truth.assertThat(repository.count()).isEqualTo(0)
    Truth.assertThat(repository.getDefault()).isNull()
    Truth.assertThat(repository.defaultId).isEqualTo(-1)
  }

  @Test
  fun deleteFromMultipleDefaultFirst() = runTest(testDispatcher) {
    val repository = repository

    val settings = createSettings("192.167.90.10")
    val settings1 = createSettings("192.167.90.11")
    val settings2 = createSettings("192.167.90.12")
    val settings3 = createSettings("192.167.90.14")

    repository.save(settings)
    repository.save(settings1)
    repository.save(settings2)
    repository.save(settings3)

    Truth.assertThat(repository.count()).isEqualTo(4)

    Truth.assertThat(settings.id).isEqualTo(1)
    Truth.assertThat(repository.getDefault()).isEqualTo(settings)

    repository.delete(settings)

    Truth.assertThat(repository.count()).isEqualTo(3)
    Truth.assertThat(repository.getDefault()).isEqualTo(settings1)
    Truth.assertThat(repository.defaultId).isEqualTo(2)
  }

  @Test
  fun deleteFromMultipleDefaultSecond() = runTest(testDispatcher) {
    val repository = repository

    val settings = createSettings("192.167.90.10")
    val settings1 = createSettings("192.167.90.11")
    val settings2 = createSettings("192.167.90.12")
    val settings3 = createSettings("192.167.90.14")

    repository.save(settings)
    repository.save(settings1)
    repository.save(settings2)
    repository.save(settings3)

    Truth.assertThat(repository.count()).isEqualTo(4)

    Truth.assertThat(settings.id).isEqualTo(1)
    Truth.assertThat(repository.getDefault()).isEqualTo(settings)

    repository.setDefault(settings1)
    Truth.assertThat(repository.getDefault()).isEqualTo(settings1)

    repository.delete(settings1)

    Truth.assertThat(repository.count()).isEqualTo(3)
    Truth.assertThat(repository.getDefault()).isEqualTo(settings)
    Truth.assertThat(repository.defaultId).isEqualTo(1)
  }

  @Test
  fun deleteFromMultipleDefaultLast() = runTest(testDispatcher) {
    val repository = repository

    val settings = createSettings("192.167.90.10")
    val settings1 = createSettings("192.167.90.11")
    val settings2 = createSettings("192.167.90.12")
    val settings3 = createSettings("192.167.90.14")

    repository.save(settings)
    repository.save(settings1)
    repository.save(settings2)
    repository.save(settings3)

    Truth.assertThat(repository.count()).isEqualTo(4)

    Truth.assertThat(settings.id).isEqualTo(1)
    Truth.assertThat(repository.getDefault()).isEqualTo(settings)

    repository.setDefault(settings3)
    Truth.assertThat(repository.getDefault()).isEqualTo(settings3)

    repository.delete(settings3)

    Truth.assertThat(repository.count()).isEqualTo(3)
    Truth.assertThat(repository.getDefault()).isEqualTo(settings2)
    Truth.assertThat(repository.defaultId).isEqualTo(3)
  }

  @Test
  fun deleteFromMultipleNonDefault() = runTest(testDispatcher) {
    val repository = repository

    val settings = createSettings("192.167.90.10")
    val settings1 = createSettings("192.167.90.11")
    val settings2 = createSettings("192.167.90.12")
    val settings3 = createSettings("192.167.90.14")

    repository.save(settings)
    repository.save(settings1)
    repository.save(settings2)
    repository.save(settings3)

    Truth.assertThat(repository.count()).isEqualTo(4)

    Truth.assertThat(settings.id).isEqualTo(1)
    Truth.assertThat(repository.getDefault()).isEqualTo(settings)

    repository.setDefault(settings3)
    Truth.assertThat(repository.getDefault()).isEqualTo(settings3)

    repository.delete(settings1)

    Truth.assertThat(repository.count()).isEqualTo(3)
    Truth.assertThat(repository.getDefault()).isEqualTo(settings3)
    Truth.assertThat(repository.defaultId).isEqualTo(4)
  }

  @Test
  fun updateSettings() = runTest(testDispatcher) {
    val newPort = 6060
    val address = "192.167.90.10"
    val newAddress = "192.167.90.11"

    val repository = repository

    val settings = createSettings(address)
    repository.save(settings)

    Truth.assertThat(settings.id).isEqualTo(1)
    val defaultSettings = repository.getDefault()

    Truth.assertThat(defaultSettings).isEqualTo(settings)
    Truth.assertThat(defaultSettings!!.port).isEqualTo(3000)
    Truth.assertThat(defaultSettings.address).isEqualTo(address)

    settings.port = newPort

    repository.update(settings)

    Truth.assertThat(repository.getDefault()!!.port).isEqualTo(newPort)

    settings.address = newAddress
    repository.update(settings)

    Truth.assertThat(repository.getDefault()!!.address).isEqualTo(newAddress)
  }

  private fun createSettings(address: String): ConnectionSettings {
    val settings = ConnectionSettings()
    settings.name = "Desktop PC"
    settings.address = address
    settings.port = 3000
    return settings
  }

  private inner class TestModule
  @SuppressLint("CommitPrefEdits") constructor() : Module() {
    init {
      bind(SharedPreferences::class.java).toProviderInstance {
        val defaultId = longArrayOf(-1)
        val preferences = Mockito.mock(SharedPreferences::class.java)
        val editor = Mockito.mock(SharedPreferences.Editor::class.java)
        Mockito.`when`(preferences.edit()).thenReturn(editor)
        Mockito.`when`(preferences.getLong(ArgumentMatchers.anyString(), ArgumentMatchers.anyLong()))
          .thenAnswer { defaultId[0] }
        Mockito.`when`(editor.putLong(ArgumentMatchers.anyString(), ArgumentMatchers.anyLong())).then {
          val o = it.arguments[1]
          defaultId[0] = o.toString().toLong()
          editor
        }
        preferences
      }
      bind(ConnectionRepository::class.java).to(ConnectionRepositoryImpl::class.java)
      bind(Resources::class.java).toProviderInstance {
        val resources = Mockito.mock(Resources::class.java)
        Mockito.`when`(resources.getString(ArgumentMatchers.anyInt())).thenReturn("preferences_key")
        resources
      }
    }
  }

  @After
  @Throws(Exception::class)
  fun tearDown() {
    Toothpick.reset()
  }
}
