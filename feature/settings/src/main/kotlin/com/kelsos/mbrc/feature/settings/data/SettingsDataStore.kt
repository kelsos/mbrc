package com.kelsos.mbrc.feature.settings.data

import android.content.Context
import androidx.datastore.core.DataMigration
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.SharedPreferencesMigration
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore

object SettingsDataStore {
  private const val SETTINGS_NAME = "settings"
  private const val LAST_VERSION_RUN_KEY_NAME = "last_version_run"

  val Context.dataStore: DataStore<Preferences> by preferencesDataStore(
    name = SETTINGS_NAME,
    produceMigrations = { context ->
      listOf(
        SharedPreferencesMigration(
          context = context,
          sharedPreferencesName = context.packageName + "_preferences"
        ),
        lastVersionRunTypeMigration()
      )
    }
  )

  // last_version_run was historically stored as Long in SharedPreferences and
  // migrated verbatim into DataStore; newer code declared it as Int, crashing
  // with ClassCastException when reading a migrated value. Normalize to Long.
  internal fun lastVersionRunTypeMigration(): DataMigration<Preferences> =
    object : DataMigration<Preferences> {
      override suspend fun shouldMigrate(currentData: Preferences): Boolean {
        val entry = currentData.asMap().entries
          .firstOrNull { it.key.name == LAST_VERSION_RUN_KEY_NAME }
        return entry != null && entry.value !is Long
      }

      override suspend fun migrate(currentData: Preferences): Preferences {
        val mutable = currentData.toMutablePreferences()
        val entry = currentData.asMap().entries
          .firstOrNull { it.key.name == LAST_VERSION_RUN_KEY_NAME }
        if (entry != null && entry.value !is Long) {
          val asLong = (entry.value as? Number)?.toLong() ?: 0L
          @Suppress("UNCHECKED_CAST")
          mutable.remove(entry.key as Preferences.Key<Any>)
          mutable[longPreferencesKey(LAST_VERSION_RUN_KEY_NAME)] = asLong
        }
        return mutable.toPreferences()
      }

      override suspend fun cleanUp() = Unit
    }

  object PreferenceKeys {
    val THEME = stringPreferencesKey("theme_preference")
    val DEBUG_LOGGING = booleanPreferencesKey("settings_debug_log")
    val PLUGIN_UPDATE_CHECK = booleanPreferencesKey("plugin_update_check")
    val INCOMING_CALL_ACTION = stringPreferencesKey("incoming_call_action")
    val LIBRARY_TRACK_DEFAULT_ACTION = stringPreferencesKey("mbrc.library_track_default")
    val ALBUM_ARTISTS_ONLY = booleanPreferencesKey("mbrc.settings.album_artist_only")
    val LAST_UPDATE_CHECK = longPreferencesKey("last_update_check")
    val LAST_VERSION_RUN = longPreferencesKey(LAST_VERSION_RUN_KEY_NAME)
    val REQUIRED_UPDATE_CHECK = longPreferencesKey("update_required_check")
    val CLIENT_UUID = stringPreferencesKey("uuid")
    val HALF_STAR_RATING = booleanPreferencesKey("mbrc.settings.half_star_rating")
    val SHOW_RATING_ON_PLAYER = booleanPreferencesKey("mbrc.settings.show_rating_on_player")
    val KEEP_SCREEN_ON = stringPreferencesKey("mbrc.settings.keep_screen_on_mode")

    // Library sorting preferences
    val GENRE_SORT = stringPreferencesKey("mbrc.library.sort.genre")
    val ARTIST_SORT = stringPreferencesKey("mbrc.library.sort.artist")
    val ALBUM_SORT = stringPreferencesKey("mbrc.library.sort.album")
    val TRACK_SORT = stringPreferencesKey("mbrc.library.sort.track")
    val GENRE_ARTISTS_SORT = stringPreferencesKey("mbrc.library.sort.genre_artists")
    val ARTIST_ALBUMS_SORT = stringPreferencesKey("mbrc.library.sort.artist_albums")
    val ALBUM_VIEW_MODE = stringPreferencesKey("mbrc.library.album_view_mode")
  }

  object DefaultValues {
    const val THEME = "dark"
    const val DEBUG_LOGGING = false
    const val PLUGIN_UPDATE_CHECK = false
    const val INCOMING_CALL_ACTION = "none"
    const val LIBRARY_TRACK_DEFAULT_ACTION = "now"
    const val ALBUM_ARTISTS_ONLY = false
    const val LAST_UPDATE_CHECK = 0L
    const val LAST_VERSION_RUN = 0L
    const val REQUIRED_UPDATE_CHECK = 0L
    const val HALF_STAR_RATING = true
    const val SHOW_RATING_ON_PLAYER = false
    const val KEEP_SCREEN_ON = KeepScreenOn.NEVER

    // Library sorting defaults (format: "field:order")
    const val GENRE_SORT = "name:asc"
    const val ARTIST_SORT = "name:asc"
    const val ALBUM_SORT = "name:asc"
    const val TRACK_SORT = "title:asc"
    const val GENRE_ARTISTS_SORT = "name:asc"
    const val ARTIST_ALBUMS_SORT = "name:asc"
    const val ALBUM_VIEW_MODE = "auto"
  }
}
