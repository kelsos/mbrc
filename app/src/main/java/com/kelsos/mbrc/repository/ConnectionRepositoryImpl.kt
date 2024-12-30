package com.kelsos.mbrc.repository

import android.content.SharedPreferences
import android.content.res.Resources
import com.kelsos.mbrc.R
import com.kelsos.mbrc.data.ConnectionSettings
import com.kelsos.mbrc.data.ConnectionSettings_Table
import com.kelsos.mbrc.di.modules.AppDispatchers
import com.raizlabs.android.dbflow.kotlinextensions.delete
import com.raizlabs.android.dbflow.kotlinextensions.save
import com.raizlabs.android.dbflow.kotlinextensions.update
import com.raizlabs.android.dbflow.sql.language.SQLite
import kotlinx.coroutines.withContext
import javax.inject.Inject

class ConnectionRepositoryImpl
@Inject constructor(
  private val preferences: SharedPreferences,
  private val resources: Resources,
  private val dispatchers: AppDispatchers
) : ConnectionRepository {

  override suspend fun save(settings: ConnectionSettings) = withContext(dispatchers.db) {
    settings.save()

    if (count() == 1L) {
      last?.let { settings ->
        setDefault(settings)
      }
    }
  }

  override suspend fun delete(settings: ConnectionSettings) = withContext(dispatchers.db) {
    val oldId = settings.id

    settings.delete()

    if (oldId != defaultId) {
      return@withContext
    }

    val count = count()
    if (count == 0L) {
      defaultId = -1
    } else {
      val connectionSettings = getItemBefore(oldId) ?: first
      connectionSettings?.let { settings ->
        setDefault(settings)
      }
    }
  }

  private fun getItemBefore(id: Long): ConnectionSettings? {
    return SQLite.select()
      .from(ConnectionSettings::class.java)
      .where(ConnectionSettings_Table.id.lessThan(id))
      .orderBy(ConnectionSettings_Table.id, false)
      .querySingle()
  }

  private val first: ConnectionSettings?
    get() = SQLite.select()
      .from(ConnectionSettings::class.java)
      .orderBy(ConnectionSettings_Table.id, true)
      .querySingle()

  private val last: ConnectionSettings?
    get() = SQLite.select()
      .from(ConnectionSettings::class.java)
      .orderBy(ConnectionSettings_Table.id, false)
      .querySingle()

  override suspend fun update(settings: ConnectionSettings) = withContext(dispatchers.db) {
    settings.update()
  }

  override suspend fun setDefault(settings: ConnectionSettings) {
    defaultId = settings.id
  }

  override suspend fun getDefault(): ConnectionSettings? = withContext(dispatchers.db) {
    val defaultId = defaultId
    if (defaultId < 0) {
      return@withContext null
    }

    return@withContext SQLite.select()
      .from(ConnectionSettings::class.java)
      .where(ConnectionSettings_Table.id.`is`(defaultId))
      .querySingle()

  }

  override var defaultId: Long
    get() {
      val key = resources.getString(R.string.settings_key_default_index)
      return this.preferences.getLong(key, 0)
    }
    private set(id) {
      val key = resources.getString(R.string.settings_key_default_index)
      this.preferences.edit().putLong(key, id).apply()
    }

  override suspend fun getAll(): List<ConnectionSettings>  = withContext(dispatchers.db) {
    return@withContext SQLite.select().from(ConnectionSettings::class.java).queryList()
  }

  override suspend fun count(): Long = withContext(dispatchers.db) {
    return@withContext SQLite.selectCountOf().from(ConnectionSettings::class.java).longValue()
  }

}
