package com.kelsos.mbrc.features.settings

import android.app.Application
import android.content.SharedPreferences
import androidx.annotation.StringDef
import com.kelsos.mbrc.R
import com.kelsos.mbrc.common.utilities.RemoteUtils.getVersionCode
import com.kelsos.mbrc.logging.FileLoggingTree
import timber.log.Timber
import java.time.Instant

class SettingsManagerImpl(
  private val context: Application,
  private val preferences: SharedPreferences,
) : SettingsManager {
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

  private fun loggingEnabled(): Boolean = preferences.getBoolean(getKey(R.string.settings_key_debug_logging), false)

  @SettingsManager.CallAction
  override fun getCallAction(): String =
    preferences.getString(
      getKey(R.string.settings_key_incoming_call_action),
      SettingsManager.Companion.NONE,
    ) ?: SettingsManager.Companion.NONE

  override fun isPluginUpdateCheckEnabled(): Boolean = preferences.getBoolean(getKey(R.string.settings_key_plugin_check), false)

  override fun getLastUpdated(required: Boolean): Instant {
    val key = if (required) REQUIRED_CHECK else getKey(R.string.settings_key_last_update_check)
    return Instant.ofEpochMilli(preferences.getLong(key, 0))
  }

  override fun setLastUpdated(
    lastChecked: Instant,
    required: Boolean,
  ) {
    val key = if (required) REQUIRED_CHECK else getKey(R.string.settings_key_last_update_check)
    preferences
      .edit()
      .putLong(key, lastChecked.toEpochMilli())
      .apply()
  }

  override suspend fun shouldDisplayOnlyAlbumArtists(): Boolean =
    preferences.getBoolean(getKey(R.string.settings_key_album_artists_only), false)

  override fun setShouldDisplayOnlyAlbumArtist(onlyAlbumArtist: Boolean) {
    preferences
      .edit()
      .putBoolean(getKey(R.string.settings_key_album_artists_only), onlyAlbumArtist)
      .apply()
  }

  override fun shouldShowChangeLog(): Boolean {
    val lastVersionCode = preferences.getLong(getKey(R.string.settings_key_last_version_run), 0)
    val currentVersion = context.getVersionCode()

    if (lastVersionCode < currentVersion) {
      preferences
        .edit()
        .putLong(getKey(R.string.settings_key_last_version_run), currentVersion)
        .apply()
      Timber.Forest.d("Update or fresh install")

      return true
    }
    return false
  }

  private fun getKey(settingsKey: Int) = context.getString(settingsKey)

  companion object {
    const val REQUIRED_CHECK = "update_required_check"
  }
}

interface SettingsManager {
  @CallAction fun getCallAction(): String

  @StringDef(
    NONE,
    PAUSE,
    STOP,
    REDUCE,
  )
  @Retention(AnnotationRetention.SOURCE)
  annotation class CallAction

  companion object {
    const val NONE = "none"
    const val PAUSE = "pause"
    const val STOP = "stop"
    const val REDUCE = "reduce"
  }

  suspend fun shouldDisplayOnlyAlbumArtists(): Boolean

  fun setShouldDisplayOnlyAlbumArtist(onlyAlbumArtist: Boolean)

  fun shouldShowChangeLog(): Boolean

  fun isPluginUpdateCheckEnabled(): Boolean

  fun getLastUpdated(required: Boolean = false): Instant

  fun setLastUpdated(
    lastChecked: Instant,
    required: Boolean = false,
  )
}
