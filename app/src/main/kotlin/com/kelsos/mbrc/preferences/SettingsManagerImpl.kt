package com.kelsos.mbrc.preferences

import android.app.Application
import android.content.SharedPreferences
import com.kelsos.mbrc.R
import com.kelsos.mbrc.logging.FileLoggingTree
import com.kelsos.mbrc.preferences.SettingsManager.CallAction
import com.kelsos.mbrc.preferences.SettingsManager.Companion.NONE
import com.kelsos.mbrc.preferences.SettingsManager.Companion.REDUCE
import com.kelsos.mbrc.utilities.RemoteUtils
import io.reactivex.Single
import timber.log.Timber
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SettingsManagerImpl
@Inject
constructor(
  private val context: Application,
  private val preferences: SharedPreferences
) : SettingsManager {

  init {
    setupManager()
  }

  private fun setupManager() {
    updatePreferences()
    val loggingEnabled = loggingEnabled()
    if (loggingEnabled) {
      Timber.plant(FileLoggingTree(this.context.applicationContext))
    } else {
      val fileLoggingTree = Timber.forest().find { it is FileLoggingTree }
      fileLoggingTree?.let { Timber.uproot(it) }
    }
  }

  private fun loggingEnabled(): Boolean {
    return preferences.getBoolean(getKey(R.string.settings_key_debug_logging), false)
  }

  private fun updatePreferences() {
    val enabled = preferences.getBoolean(getKey(R.string.settings_legacy_key_reduce_volume), false)
    if (enabled) {
      preferences.edit().putString(getKey(R.string.settings_key_incoming_call_action), REDUCE).apply()
    }
  }

  override fun isNotificationControlEnabled(): Boolean {
    return preferences.getBoolean(getKey(R.string.settings_key_notification_control), true)
  }

  @CallAction override fun getCallAction(): String = preferences.getString(
      getKey(R.string.settings_key_incoming_call_action), NONE)

  override fun isPluginUpdateCheckEnabled(): Boolean {
    return preferences.getBoolean(getKey(R.string.settings_key_plugin_check), false)
  }

  override fun getLastUpdated(): Date {
    return Date(preferences.getLong(getKey(R.string.settings_key_last_update_check), 0))
  }

  override fun setLastUpdated(lastChecked: Date) {
    preferences.edit()
        .putLong(getKey(R.string.settings_key_last_update_check), lastChecked.time)
        .apply()
  }

  override fun shouldDisplayOnlyAlbumArtists(): Single<Boolean> {
    return Single.fromCallable {
      return@fromCallable preferences.getBoolean(getKey(R.string.settings_key_album_artists_only), false)
    }
  }

  override fun setShouldDisplayOnlyAlbumArtist(onlyAlbumArtist: Boolean) {
    preferences.edit().putBoolean(getKey(R.string.settings_key_album_artists_only), onlyAlbumArtist).apply()
  }

  override fun shouldShowChangeLog(): Single<Boolean> {
    return Single.fromCallable {

      val lastVersionCode = preferences.getLong(getKey(R.string.settings_key_last_version_run), 0)
      val currentVersion = RemoteUtils.getVersionCode(context)

      if (lastVersionCode < currentVersion) {
        preferences.edit()
            .putLong(getKey(R.string.settings_key_last_version_run), currentVersion)
            .apply()
        Timber.d("Update or fresh install")

        return@fromCallable true
      }
      return@fromCallable false
    }
  }

  private fun getKey(settingsKey: Int) = context.getString(settingsKey)
}