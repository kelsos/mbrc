package com.kelsos.mbrc.data

import android.content.SharedPreferences
import androidx.core.content.edit
import com.kelsos.mbrc.common.utilities.AppCoroutineDispatchers
import com.kelsos.mbrc.features.settings.ConnectionDao
import kotlinx.coroutines.withContext
import timber.log.Timber

/**
 * Migrates the default connection setting from SharedPreferences to the database.
 * This is a one-time migration that runs after database migration 3->4.
 */
class DefaultConnectionMigration(
  private val sharedPreferences: SharedPreferences,
  private val connectionDao: ConnectionDao,
  private val appDispatchers: AppCoroutineDispatchers
) {
  suspend fun migrate() {
    withContext(appDispatchers.io) {
      try {
        // Check if the old key exists
        if (!sharedPreferences.contains(OLD_DEFAULT_KEY)) {
          Timber.d("No default connection key found in SharedPreferences, nothing to migrate")
          return@withContext
        }

        val defaultId = sharedPreferences.getLong(OLD_DEFAULT_KEY, -1L)
        if (defaultId == -1L) {
          Timber.w("Default connection ID is invalid, nothing to migrate")
          return@withContext
        }

        val connection = connectionDao.getById(defaultId)
        if (connection != null) {
          // Set it as default in the database
          connectionDao.updateDefault(defaultId)
          Timber.d("Migrated default connection ID $defaultId to database")
        } else {
          Timber.w("Default connection ID $defaultId not found in database")
        }

        // Remove the old key from SharedPreferences
        sharedPreferences.edit {
          remove(OLD_DEFAULT_KEY)
        }
        Timber.d("Removed old default connection key from SharedPreferences")
      } catch (@Suppress("TooGenericExceptionCaught") e: Exception) {
        Timber.e(e, "Failed to migrate default connection to database")
      }
    }
  }

  companion object {
    private const val OLD_DEFAULT_KEY = "mbrc_default_settings"
  }
}
