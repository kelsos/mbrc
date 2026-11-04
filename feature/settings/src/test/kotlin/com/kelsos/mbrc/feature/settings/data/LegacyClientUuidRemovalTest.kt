package com.kelsos.mbrc.feature.settings.data

import androidx.datastore.preferences.core.mutablePreferencesOf
import com.google.common.truth.Truth.assertThat
import com.google.common.truth.Truth.assertWithMessage
import com.kelsos.mbrc.feature.settings.data.SettingsDataStore.LEGACY_CLIENT_UUID
import com.kelsos.mbrc.feature.settings.data.SettingsDataStore.PreferenceKeys
import kotlinx.coroutines.test.runTest
import org.junit.Test

class LegacyClientUuidRemovalTest {

  private val migration = SettingsDataStore.legacyClientUuidRemoval()

  @Test
  fun `shouldMigrate is false when the legacy id is absent`() = runTest {
    val prefs = mutablePreferencesOf(PreferenceKeys.THEME to "dark").toPreferences()

    assertThat(migration.shouldMigrate(prefs)).isFalse()
  }

  @Test
  fun `migrate drops only the legacy id`() = runTest {
    val prefs = mutablePreferencesOf(
      LEGACY_CLIENT_UUID to "restored-from-another-phone",
      PreferenceKeys.THEME to "dark"
    ).toPreferences()

    assertThat(migration.shouldMigrate(prefs)).isTrue()
    val migrated = migration.migrate(prefs)

    assertWithMessage("the other settings survive")
      .that(migrated.asMap().keys)
      .containsExactly(PreferenceKeys.THEME)
    assertWithMessage("nothing left to migrate")
      .that(migration.shouldMigrate(migrated))
      .isFalse()
  }
}
