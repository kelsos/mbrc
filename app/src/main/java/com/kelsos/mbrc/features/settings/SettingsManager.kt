package com.kelsos.mbrc.features.settings

import android.app.Application
import android.content.SharedPreferences
import androidx.core.content.edit
import com.kelsos.mbrc.R
import com.kelsos.mbrc.common.utilities.RemoteUtils
import com.kelsos.mbrc.logging.FileLoggingTree
import java.time.Instant
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import timber.log.Timber

class SettingsManagerImpl(
  private val context: Application,
  private val preferences: SharedPreferences
) : SettingsManager {
  private val mutableShouldDisplayOnlyArtists = MutableStateFlow(shouldDisplayOnlyAlbumArtists())
  override val shouldDisplayOnlyArtists: Flow<Boolean> = mutableShouldDisplayOnlyArtists

  init {
    setupManager()
  }

  private fun setupManager() {
    val loggingEnabled = loggingEnabled()
    if (loggingEnabled) {
      Timber.Forest.plant(FileLoggingTree(this.context.applicationContext))
    } else {
      val fileLoggingTree = Timber.Forest.forest().find { it is FileLoggingTree }
      fileLoggingTree?.let { Timber.Forest.uproot(it) }
    }
  }

  private fun loggingEnabled(): Boolean =
    preferences.getBoolean(getKey(R.string.settings_key_debug_logging), false)

  override fun getCallAction(): CallAction {
    val key = getKey(R.string.settings_key_incoming_call_action)
    return CallAction.fromString(preferences.getString(key, CallAction.NONE) ?: CallAction.NONE)
  }

  override fun isPluginUpdateCheckEnabled(): Boolean {
    val key = getKey(R.string.settings_key_plugin_check)
    return preferences.getBoolean(key, false)
  }

  override fun getLastUpdated(required: Boolean): Instant {
    val key = if (required) REQUIRED_CHECK else getKey(R.string.settings_key_last_update_check)
    return Instant.ofEpochMilli(preferences.getLong(key, 0))
  }

  override fun setLastUpdated(lastChecked: Instant, required: Boolean) {
    val key = if (required) REQUIRED_CHECK else getKey(R.string.settings_key_last_update_check)
    preferences.edit {
      putLong(key, lastChecked.toEpochMilli())
    }
  }

  private fun shouldDisplayOnlyAlbumArtists(): Boolean {
    val key = getKey(R.string.settings_key_album_artists_only)
    return preferences.getBoolean(key, false)
  }

  override fun setShouldDisplayOnlyAlbumArtist(onlyAlbumArtist: Boolean) {
    preferences.edit {
      putBoolean(getKey(R.string.settings_key_album_artists_only), onlyAlbumArtist)
    }

    mutableShouldDisplayOnlyArtists.tryEmit(onlyAlbumArtist)
  }

  override fun shouldShowChangeLog(): Boolean {
    val lastVersionCode = preferences.getInt(getKey(R.string.settings_key_last_version_run), 0)
    val currentVersion = RemoteUtils.VERSION_CODE

    if (lastVersionCode < currentVersion) {
      preferences.edit {
        putInt(getKey(R.string.settings_key_last_version_run), currentVersion)
      }
      Timber.Forest.d("Update or fresh install")

      return true
    }
    return false
  }

  override fun getThemePreference(): String {
    val key = getKey(R.string.settings_key_theme)
    return preferences.getString(key, "dark") ?: "dark"
  }

  override fun setThemePreference(theme: String) {
    preferences.edit {
      putString(getKey(R.string.settings_key_theme), theme)
    }
  }

  private fun getKey(settingsKey: Int) = context.getString(settingsKey)

  companion object {
    const val REQUIRED_CHECK = "update_required_check"
  }
}

interface SettingsManager {
  fun getCallAction(): CallAction

  val shouldDisplayOnlyArtists: Flow<Boolean>

  fun setShouldDisplayOnlyAlbumArtist(onlyAlbumArtist: Boolean)

  fun shouldShowChangeLog(): Boolean

  fun isPluginUpdateCheckEnabled(): Boolean

  fun getLastUpdated(required: Boolean = false): Instant

  fun setLastUpdated(lastChecked: Instant, required: Boolean = false)

  fun getThemePreference(): String

  fun setThemePreference(theme: String)
}
