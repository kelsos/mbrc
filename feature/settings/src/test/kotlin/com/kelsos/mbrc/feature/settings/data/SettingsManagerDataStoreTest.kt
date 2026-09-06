package com.kelsos.mbrc.feature.settings.data

import android.app.Application
import androidx.datastore.preferences.core.edit
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import com.google.common.truth.Truth.assertWithMessage
import com.kelsos.mbrc.core.common.test.testDispatchers
import com.kelsos.mbrc.core.common.utilities.AppInfo
import com.kelsos.mbrc.feature.settings.data.SettingsDataStore.dataStore
import com.kelsos.mbrc.feature.settings.domain.SettingsManager
import java.time.Instant
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * The data store is a process singleton keyed by the application context, so every test starts by
 * clearing it: without that, a version written by one test would decide the outcome of the next.
 */
@RunWith(AndroidJUnit4::class)
class SettingsManagerDataStoreTest {

  private val context: Application = ApplicationProvider.getApplicationContext()

  @Before
  fun setUp() {
    runBlocking { context.dataStore.edit { it.clear() } }
  }

  @Test
  fun `the changelog is shown on a fresh install`() = runTest {
    val manager = manager(versionCode = 130)

    assertThat(manager.checkShouldShowChangeLog()).isTrue()
  }

  @Test
  fun `the changelog is shown once per version`() = runTest {
    val manager = manager(versionCode = 130)

    manager.checkShouldShowChangeLog()

    assertWithMessage("the version was recorded by the first check")
      .that(manager.checkShouldShowChangeLog())
      .isFalse()
  }

  @Test
  fun `the changelog is shown again after an update`() = runTest {
    manager(versionCode = 130).checkShouldShowChangeLog()

    assertThat(manager(versionCode = 131).checkShouldShowChangeLog()).isTrue()
  }

  @Test
  fun `a downgrade does not show the changelog`() = runTest {
    manager(versionCode = 131).checkShouldShowChangeLog()

    assertThat(manager(versionCode = 130).checkShouldShowChangeLog()).isFalse()
  }

  @Test
  fun `an update check that never ran reads as the epoch`() = runTest {
    val manager = manager()

    assertThat(manager.getLastUpdated(required = false)).isEqualTo(Instant.EPOCH)
    assertThat(manager.getLastUpdated(required = true)).isEqualTo(Instant.EPOCH)
  }

  @Test
  fun `the required and optional update checks are stored separately`() = runTest {
    val manager = manager()
    val optional = Instant.ofEpochMilli(1_000)
    val required = Instant.ofEpochMilli(2_000)

    manager.setLastUpdated(optional, required = false)
    manager.setLastUpdated(required, required = true)

    assertThat(manager.getLastUpdated(required = false)).isEqualTo(optional)
    assertThat(manager.getLastUpdated(required = true)).isEqualTo(required)
  }

  @Test
  fun `writing the optional check leaves the required one untouched`() = runTest {
    val manager = manager()
    val required = Instant.ofEpochMilli(2_000)

    manager.setLastUpdated(required, required = true)
    manager.setLastUpdated(Instant.ofEpochMilli(5_000), required = false)

    assertThat(manager.getLastUpdated(required = true)).isEqualTo(required)
  }

  private fun manager(versionCode: Int = 130): SettingsManager = SettingsManagerDataStore(
    context = context,
    appDispatchers = testDispatchers,
    appInfo = appInfo(versionCode)
  )

  private fun appInfo(versionCode: Int): AppInfo = object : AppInfo {
    override val versionName: String = "1.7.0"
    override val versionCode: Int = versionCode
    override val buildTime: String = ""
    override val gitRevision: String = ""
    override val applicationId: String = "com.kelsos.mbrc"
  }
}
