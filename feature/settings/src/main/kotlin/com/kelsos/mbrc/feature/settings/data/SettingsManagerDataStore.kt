package com.kelsos.mbrc.feature.settings.data

import android.app.Application
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import com.kelsos.mbrc.core.common.settings.AlbumSortField
import com.kelsos.mbrc.core.common.settings.AlbumSortPreference
import com.kelsos.mbrc.core.common.settings.ArtistSortField
import com.kelsos.mbrc.core.common.settings.ArtistSortPreference
import com.kelsos.mbrc.core.common.settings.GenreSortField
import com.kelsos.mbrc.core.common.settings.GenreSortPreference
import com.kelsos.mbrc.core.common.settings.SortPreference
import com.kelsos.mbrc.core.common.settings.TrackAction
import com.kelsos.mbrc.core.common.utilities.AppInfo
import com.kelsos.mbrc.core.common.utilities.coroutines.AppCoroutineDispatchers
import com.kelsos.mbrc.core.common.utilities.logging.FileLoggingTree
import com.kelsos.mbrc.feature.settings.data.SettingsDataStore.DefaultValues
import com.kelsos.mbrc.feature.settings.data.SettingsDataStore.PreferenceKeys
import com.kelsos.mbrc.feature.settings.data.SettingsDataStore.dataStore
import com.kelsos.mbrc.feature.settings.domain.SettingsManager
import com.kelsos.mbrc.feature.settings.theme.Theme
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
  private val appDispatchers: AppCoroutineDispatchers,
  private val appInfo: AppInfo
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

  override val genreSortPreferenceFlow: Flow<GenreSortPreference> = dataStore.data.map { prefs ->
    val encoded = prefs[PreferenceKeys.GENRE_SORT] ?: DefaultValues.GENRE_SORT
    SortPreference.decode(encoded, GenreSortField::fromString, GenreSortField.NAME)
  }

  override val artistSortPreferenceFlow: Flow<ArtistSortPreference> = dataStore.data.map { prefs ->
    val encoded = prefs[PreferenceKeys.ARTIST_SORT] ?: DefaultValues.ARTIST_SORT
    SortPreference.decode(encoded, ArtistSortField::fromString, ArtistSortField.NAME)
  }

  override val albumSortPreferenceFlow: Flow<AlbumSortPreference> = dataStore.data.map { prefs ->
    val encoded = prefs[PreferenceKeys.ALBUM_SORT] ?: DefaultValues.ALBUM_SORT
    SortPreference.decode(encoded, AlbumSortField::fromString, AlbumSortField.NAME)
  }

  override val genreArtistsSortPreferenceFlow: Flow<ArtistSortPreference> =
    dataStore.data.map { prefs ->
      val encoded = prefs[PreferenceKeys.GENRE_ARTISTS_SORT] ?: DefaultValues.GENRE_ARTISTS_SORT
      SortPreference.decode(encoded, ArtistSortField::fromString, ArtistSortField.NAME)
    }

  override val artistAlbumsSortPreferenceFlow: Flow<AlbumSortPreference> =
    dataStore.data.map { prefs ->
      val encoded = prefs[PreferenceKeys.ARTIST_ALBUMS_SORT] ?: DefaultValues.ARTIST_ALBUMS_SORT
      SortPreference.decode(encoded, AlbumSortField::fromString, AlbumSortField.YEAR)
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

  override suspend fun setGenreSortPreference(preference: GenreSortPreference) {
    dataStore.edit { preferences ->
      preferences[PreferenceKeys.GENRE_SORT] =
        SortPreference.encode(preference) { it.value }
    }
  }

  override suspend fun setArtistSortPreference(preference: ArtistSortPreference) {
    dataStore.edit { preferences ->
      preferences[PreferenceKeys.ARTIST_SORT] =
        SortPreference.encode(preference) { it.value }
    }
  }

  override suspend fun setAlbumSortPreference(preference: AlbumSortPreference) {
    dataStore.edit { preferences ->
      preferences[PreferenceKeys.ALBUM_SORT] =
        SortPreference.encode(preference) { it.value }
    }
  }

  override suspend fun setGenreArtistsSortPreference(preference: ArtistSortPreference) {
    dataStore.edit { preferences ->
      preferences[PreferenceKeys.GENRE_ARTISTS_SORT] =
        SortPreference.encode(preference) { it.value }
    }
  }

  override suspend fun setArtistAlbumsSortPreference(preference: AlbumSortPreference) {
    dataStore.edit { preferences ->
      preferences[PreferenceKeys.ARTIST_ALBUMS_SORT] =
        SortPreference.encode(preference) { it.value }
    }
  }

  override suspend fun checkShouldShowChangeLog(): Boolean {
    val lastVersionCode = dataStore.data.map { preferences ->
      preferences[PreferenceKeys.LAST_VERSION_RUN] ?: DefaultValues.LAST_VERSION_RUN
    }.first()

    val currentVersion = appInfo.versionCode

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
