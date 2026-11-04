package com.kelsos.mbrc.feature.settings.data

import android.app.Application
import androidx.datastore.preferences.core.edit
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import com.google.common.truth.Truth.assertWithMessage
import com.kelsos.mbrc.feature.settings.data.SettingsDataStore.LEGACY_CLIENT_UUID
import com.kelsos.mbrc.feature.settings.data.SettingsDataStore.dataStore
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * DataStore allows one active instance per file in a process, so every test shares [subject] and
 * starts by clearing it.
 */
@RunWith(AndroidJUnit4::class)
class ClientInformationStoreImplTest {

  private val context: Application = ApplicationProvider.getApplicationContext()

  @Before
  fun setUp() {
    runBlocking {
      subject.store.edit { it.clear() }
      context.dataStore.edit { it.clear() }
    }
  }

  @Test
  fun `the client id is generated once and kept`() = runTest {
    val first = subject.getClientId()

    assertThat(first).isNotEmpty()
    assertWithMessage("a second read returns the stored id")
      .that(subject.getClientId())
      .isEqualTo(first)
  }

  @Test
  fun `the client id never comes from or goes to the backed up settings file`() = runTest {
    val restored = "restored-from-another-phone"
    context.dataStore.edit { it[LEGACY_CLIENT_UUID] = restored }

    assertWithMessage("an id restored with the settings is not reused")
      .that(subject.getClientId())
      .isNotEqualTo(restored)
    assertWithMessage("the settings file holds nothing new")
      .that(context.dataStore.data.first().asMap().keys)
      .containsExactly(LEGACY_CLIENT_UUID)
  }

  private companion object {
    val subject by lazy { ClientInformationStoreImpl(ApplicationProvider.getApplicationContext()) }
  }
}
