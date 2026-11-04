package com.kelsos.mbrc.feature.settings.data

import android.app.Application
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import java.io.File
import java.util.UUID
import kotlinx.coroutines.flow.first

fun interface ClientInformationStore {
  suspend fun getClientId(): String
}

/**
 * Keeps the client id in `noBackupFilesDir`, which Android leaves out of every cloud backup and
 * device transfer whatever the backup rules say. The plugin treats a second connection with the
 * same id as a reconnect and drops the first, so two phones restored from one backup would keep
 * disconnecting each other (#416).
 */
class ClientInformationStoreImpl(context: Application) : ClientInformationStore {
  internal val store: DataStore<Preferences> = PreferenceDataStoreFactory.create(
    produceFile = { File(context.noBackupFilesDir, FILE_NAME) }
  )

  override suspend fun getClientId(): String = store.data.first()[CLIENT_ID] ?: generateClientId()

  private suspend fun generateClientId(): String {
    val stored = store.edit { preferences ->
      if (CLIENT_ID !in preferences) preferences[CLIENT_ID] = UUID.randomUUID().toString()
    }
    return checkNotNull(stored[CLIENT_ID])
  }

  private companion object {
    const val FILE_NAME = "client_identity.preferences_pb"
    val CLIENT_ID = stringPreferencesKey("client_id")
  }
}
