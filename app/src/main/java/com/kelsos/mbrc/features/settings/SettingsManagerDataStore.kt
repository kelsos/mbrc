package com.kelsos.mbrc.features.settings

import android.app.Application
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import com.kelsos.mbrc.common.utilities.AppCoroutineDispatchers
import com.kelsos.mbrc.common.utilities.RemoteUtils
import com.kelsos.mbrc.features.settings.SettingsDataStore.DefaultValues
import com.kelsos.mbrc.features.settings.SettingsDataStore.PreferenceKeys
import com.kelsos.mbrc.features.settings.SettingsDataStore.dataStore
import com.kelsos.mbrc.features.theme.Theme
import com.kelsos.mbrc.logging.FileLoggingTree
import java.time.Instant
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import timber.log.Timber

class SettingsManagerDataStore(
  private val context: Application,
  private val appDispatchers: AppCoroutineDispatchers
) : SettingsManager {

  private val dataStore: DataStore<Preferences> = context.dataStore
  private val scope = CoroutineScope(SupervisorJob() + appDispatchers.io)

  init {
    setupManager()
  }

  private fun setupManager() {
    scope.launch {
      dataStore.data.map { preferences ->
        preferences[PreferenceKeys.DEBUG_LOGGING] ?: DefaultValues.DEBUG_LOGGING
      }.collect { enabled ->
        if (enabled) {
          Timber.forest().find { it is FileLoggingTree }
            ?: Timber.plant(FileLoggingTree(context.applicationContext))
        } else {
          Timber.forest().find { it is FileLoggingTree }?.let {
            Timber.uproot(it)
          }
        }
      }
    }
  }

  override val themeFlow: Flow<Theme> = dataStore.data.map { preferences ->
    val themeString = preferences[PreferenceKeys.THEME] ?: DefaultValues.THEME
    Theme.fromString(themeString)
  }

  override val debugLoggingFlow: Flow<Boolean> = dataStore.data.map { preferences ->
    preferences[PreferenceKeys.DEBUG_LOGGING] ?: DefaultValues.DEBUG_LOGGING
  }

  override val pluginUpdateCheckFlow: Flow<Boolean> = dataStore.data.map { preferences ->
    preferences[PreferenceKeys.PLUGIN_UPDATE_CHECK] ?: DefaultValues.PLUGIN_UPDATE_CHECK
  }

  override val incomingCallActionFlow: Flow<CallAction> = dataStore.data.map { preferences ->
    val actionString =
      preferences[PreferenceKeys.INCOMING_CALL_ACTION] ?: DefaultValues.INCOMING_CALL_ACTION
    CallAction.fromString(actionString)
  }

  override val libraryTrackDefaultActionFlow: Flow<TrackAction> = dataStore.data.map { prefs ->
    val actionString = prefs[PreferenceKeys.LIBRARY_TRACK_DEFAULT_ACTION]
      ?: DefaultValues.LIBRARY_TRACK_DEFAULT_ACTION
    TrackAction.fromString(actionString)
  }

  override val shouldDisplayOnlyArtists: Flow<Boolean> = dataStore.data.map { preferences ->
    preferences[PreferenceKeys.ALBUM_ARTISTS_ONLY] ?: DefaultValues.ALBUM_ARTISTS_ONLY
  }

  override suspend fun setTheme(theme: Theme) {
    dataStore.edit { preferences ->
      preferences[PreferenceKeys.THEME] = theme.value
    }
  }

  override suspend fun setDebugLogging(enabled: Boolean) {
    dataStore.edit { preferences ->
      preferences[PreferenceKeys.DEBUG_LOGGING] = enabled
    }
  }

  override suspend fun setPluginUpdateCheck(enabled: Boolean) {
    dataStore.edit { preferences ->
      preferences[PreferenceKeys.PLUGIN_UPDATE_CHECK] = enabled
    }
  }

  override suspend fun setIncomingCallAction(action: CallAction) {
    dataStore.edit { preferences ->
      preferences[PreferenceKeys.INCOMING_CALL_ACTION] = action.string
    }
  }

  override suspend fun setLibraryTrackDefaultAction(action: TrackAction) {
    dataStore.edit { preferences ->
      preferences[PreferenceKeys.LIBRARY_TRACK_DEFAULT_ACTION] = action.value
    }
  }

  override suspend fun setShouldDisplayOnlyAlbumArtist(onlyAlbumArtist: Boolean) {
    dataStore.edit { preferences ->
      preferences[PreferenceKeys.ALBUM_ARTISTS_ONLY] = onlyAlbumArtist
    }
  }

  override suspend fun checkShouldShowChangeLog(): Boolean {
    val lastVersionCode = dataStore.data.map { preferences ->
      preferences[PreferenceKeys.LAST_VERSION_RUN] ?: DefaultValues.LAST_VERSION_RUN
    }.first()

    val currentVersion = RemoteUtils.VERSION_CODE

    return if (lastVersionCode < currentVersion) {
      dataStore.edit { preferences ->
        preferences[PreferenceKeys.LAST_VERSION_RUN] = currentVersion
      }
      Timber.d("Update or fresh install")
      true
    } else {
      false
    }
  }

  override suspend fun getLastUpdated(required: Boolean): Instant {
    val key = if (required) {
      PreferenceKeys.REQUIRED_UPDATE_CHECK
    } else {
      PreferenceKeys.LAST_UPDATE_CHECK
    }
    val millis = dataStore.data.map { preferences ->
      preferences[key] ?: 0L
    }.first()
    return Instant.ofEpochMilli(millis)
  }

  override suspend fun setLastUpdated(lastChecked: Instant, required: Boolean) {
    dataStore.edit { preferences ->
      val key = if (required) {
        PreferenceKeys.REQUIRED_UPDATE_CHECK
      } else {
        PreferenceKeys.LAST_UPDATE_CHECK
      }
      preferences[key] = lastChecked.toEpochMilli()
    }
  }
}
