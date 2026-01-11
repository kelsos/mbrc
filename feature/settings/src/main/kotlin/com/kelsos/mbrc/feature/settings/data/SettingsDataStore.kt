package com.kelsos.mbrc.feature.settings.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.SharedPreferencesMigration
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore

object SettingsDataStore {
  private const val SETTINGS_NAME = "settings"

  val Context.dataStore: DataStore<Preferences> by preferencesDataStore(
    name = SETTINGS_NAME,
    produceMigrations = { context ->
      // Migrate from default SharedPreferences
      listOf(
        SharedPreferencesMigration(
          context = context,
          sharedPreferencesName = context.packageName + "_preferences"
        )
      )
    }
  )

  object PreferenceKeys {
    val THEME = stringPreferencesKey("theme_preference")
    val DEBUG_LOGGING = booleanPreferencesKey("settings_debug_log")
    val PLUGIN_UPDATE_CHECK = booleanPreferencesKey("plugin_update_check")
    val INCOMING_CALL_ACTION = stringPreferencesKey("incoming_call_action")
    val LIBRARY_TRACK_DEFAULT_ACTION = stringPreferencesKey("mbrc.library_track_default")
    val ALBUM_ARTISTS_ONLY = booleanPreferencesKey("mbrc.settings.album_artist_only")
    val LAST_UPDATE_CHECK = longPreferencesKey("last_update_check")
    val LAST_VERSION_RUN = intPreferencesKey("last_version_run")
    val REQUIRED_UPDATE_CHECK = longPreferencesKey("update_required_check")
    val CLIENT_UUID = stringPreferencesKey("uuid")

    // Library sorting preferences
    val GENRE_SORT = stringPreferencesKey("mbrc.library.sort.genre")
    val ARTIST_SORT = stringPreferencesKey("mbrc.library.sort.artist")
    val ALBUM_SORT = stringPreferencesKey("mbrc.library.sort.album")
    val GENRE_ARTISTS_SORT = stringPreferencesKey("mbrc.library.sort.genre_artists")
    val ARTIST_ALBUMS_SORT = stringPreferencesKey("mbrc.library.sort.artist_albums")
  }

  object DefaultValues {
    const val THEME = "dark"
    const val DEBUG_LOGGING = false
    const val PLUGIN_UPDATE_CHECK = false
    const val INCOMING_CALL_ACTION = "none"
    const val LIBRARY_TRACK_DEFAULT_ACTION = "now"
    const val ALBUM_ARTISTS_ONLY = false
    const val LAST_UPDATE_CHECK = 0L
    const val LAST_VERSION_RUN = 0
    const val REQUIRED_UPDATE_CHECK = 0L

    // Library sorting defaults (format: "field:order")
    const val GENRE_SORT = "name:asc"
    const val ARTIST_SORT = "name:asc"
    const val ALBUM_SORT = "name:asc"
    const val GENRE_ARTISTS_SORT = "name:asc"
    const val ARTIST_ALBUMS_SORT = "year:asc"
  }
}
