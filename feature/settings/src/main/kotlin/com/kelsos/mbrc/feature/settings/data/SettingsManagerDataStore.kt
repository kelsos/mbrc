package com.kelsos.mbrc.feature.settings.data

import android.app.Application
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import com.kelsos.mbrc.core.common.settings.AlbumSortField
import com.kelsos.mbrc.core.common.settings.AlbumSortPreference
import com.kelsos.mbrc.core.common.settings.AlbumViewMode
import com.kelsos.mbrc.core.common.settings.ArtistSortField
import com.kelsos.mbrc.core.common.settings.ArtistSortPreference
import com.kelsos.mbrc.core.common.settings.GenreSortField
import com.kelsos.mbrc.core.common.settings.GenreSortPreference
import com.kelsos.mbrc.core.common.settings.SortPreference
import com.kelsos.mbrc.core.common.settings.TrackAction
import com.kelsos.mbrc.core.common.settings.TrackSortField
import com.kelsos.mbrc.core.common.settings.TrackSortPreference
import com.kelsos.mbrc.core.common.utilities.AppInfo
import com.kelsos.mbrc.core.common.utilities.coroutines.AppCoroutineDispatchers
import com.kelsos.mbrc.core.common.utilities.logging.FileLoggingTree
import com.kelsos.mbrc.feature.settings.data.SettingsDataStore.DefaultValues
import com.kelsos.mbrc.feature.settings.data.SettingsDataStore.PreferenceKeys
import com.kelsos.mbrc.feature.settings.data.SettingsDataStore.dataStore
import com.kelsos.mbrc.feature.settings.domain.SettingsManager
import com.kelsos.mbrc.feature.settings.theme.Theme
import java.io.IOException
import java.time.Instant
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
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

  /**
   * The only owner of the file logging tree.
   *
   * It used to share that job with a manager the settings screen called directly, so a single
   * toggle ran both: two plants could each pass the "is one already planted" check and open a
   * java.util.logging FileHandler on the same file, and the loser died on the lock file; two
   * uproots could each find the same tree, and the second threw because it had already gone. Both
   * were fatal and both showed up in Crashlytics on 1.6.1.
   *
   * Reacting to the stored value rather than to the tap also means the setting is applied on
   * startup, which is the reason this collector existed in the first place.
   */
  private fun setupManager() {
    scope.launch {
      dataStore.data.map { preferences ->
        preferences[PreferenceKeys.DEBUG_LOGGING] ?: DefaultValues.DEBUG_LOGGING
      }
        // Every write to any preference re-emits the whole set, so without this an unrelated
        // setting would replant the tree.
        .distinctUntilChanged()
        .collect { enabled ->
          if (enabled) {
            plantFileLogging()
          } else {
            Timber.forest().find { it is FileLoggingTree }?.let {
              Timber.uproot(it)
            }
          }
        }
    }
  }

  /**
   * Diagnostics are not worth a crash. The FileHandler takes a lock file next to the log and
   * throws if it cannot have it, which the app has died on in the field.
   */
  private fun plantFileLogging() {
    if (Timber.forest().any { it is FileLoggingTree }) {
      return
    }

    try {
      Timber.plant(FileLoggingTree(context.applicationContext))
    } catch (e: IOException) {
      Timber.w(e, "Could not open the log file, continuing without file logging")
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

  override val halfStarRatingFlow: Flow<Boolean> = dataStore.data.map { preferences ->
    preferences[PreferenceKeys.HALF_STAR_RATING] ?: DefaultValues.HALF_STAR_RATING
  }

  override val showRatingOnPlayerFlow: Flow<Boolean> = dataStore.data.map { preferences ->
    preferences[PreferenceKeys.SHOW_RATING_ON_PLAYER] ?: DefaultValues.SHOW_RATING_ON_PLAYER
  }

  override val keepScreenOnFlow: Flow<KeepScreenOn> = dataStore.data.map { preferences ->
    val mode = preferences[PreferenceKeys.KEEP_SCREEN_ON] ?: DefaultValues.KEEP_SCREEN_ON
    KeepScreenOn.fromString(mode)
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

  override val trackSortPreferenceFlow: Flow<TrackSortPreference> = dataStore.data.map { prefs ->
    val encoded = prefs[PreferenceKeys.TRACK_SORT] ?: DefaultValues.TRACK_SORT
    SortPreference.decode(encoded, TrackSortField::fromString, TrackSortField.TITLE)
  }

  override val genreArtistsSortPreferenceFlow: Flow<ArtistSortPreference> =
    dataStore.data.map { prefs ->
      val encoded = prefs[PreferenceKeys.GENRE_ARTISTS_SORT] ?: DefaultValues.GENRE_ARTISTS_SORT
      SortPreference.decode(encoded, ArtistSortField::fromString, ArtistSortField.NAME)
    }

  override val artistAlbumsSortPreferenceFlow: Flow<AlbumSortPreference> =
    dataStore.data.map { prefs ->
      val encoded = prefs[PreferenceKeys.ARTIST_ALBUMS_SORT] ?: DefaultValues.ARTIST_ALBUMS_SORT
      SortPreference.decode(encoded, AlbumSortField::fromString, AlbumSortField.NAME)
    }

  override val albumViewModeFlow: Flow<AlbumViewMode> = dataStore.data.map { prefs ->
    val value = prefs[PreferenceKeys.ALBUM_VIEW_MODE] ?: DefaultValues.ALBUM_VIEW_MODE
    AlbumViewMode.fromString(value)
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

  override suspend fun setHalfStarRating(enabled: Boolean) {
    dataStore.edit { preferences ->
      preferences[PreferenceKeys.HALF_STAR_RATING] = enabled
    }
  }

  override suspend fun setShowRatingOnPlayer(enabled: Boolean) {
    dataStore.edit { preferences ->
      preferences[PreferenceKeys.SHOW_RATING_ON_PLAYER] = enabled
    }
  }

  override suspend fun setKeepScreenOn(mode: KeepScreenOn) {
    dataStore.edit { preferences ->
      preferences[PreferenceKeys.KEEP_SCREEN_ON] = mode.string
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

  override suspend fun setTrackSortPreference(preference: TrackSortPreference) {
    dataStore.edit { preferences ->
      preferences[PreferenceKeys.TRACK_SORT] =
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

  override suspend fun setAlbumViewMode(mode: AlbumViewMode) {
    dataStore.edit { preferences ->
      preferences[PreferenceKeys.ALBUM_VIEW_MODE] = mode.value
    }
  }

  override suspend fun checkShouldShowChangeLog(): Boolean {
    val lastVersionCode = dataStore.data.map { preferences ->
      preferences[PreferenceKeys.LAST_VERSION_RUN] ?: DefaultValues.LAST_VERSION_RUN
    }.first()

    val currentVersion = appInfo.versionCode.toLong()

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
