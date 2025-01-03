package com.kelsos.mbrc.features.settings

import android.content.SharedPreferences
import androidx.core.content.edit
import androidx.paging.PagingData
import com.kelsos.mbrc.common.utilities.AppCoroutineDispatchers
import com.kelsos.mbrc.common.utilities.paged
import com.kelsos.mbrc.networking.discovery.DiscoveryStop
import com.kelsos.mbrc.networking.discovery.RemoteServiceDiscovery
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import timber.log.Timber

interface ConnectionRepository {
  suspend fun save(settings: ConnectionSettings)

  suspend fun delete(settings: ConnectionSettings)

  fun getAll(): Flow<PagingData<ConnectionSettings>>

  suspend fun count(): Long

  fun getDefault(): ConnectionSettings?

  fun setDefault(settings: ConnectionSettings)

  suspend fun discover(): DiscoveryStop
}

class ConnectionRepositoryImpl(
  private val dao: ConnectionDao,
  private val dispatchers: AppCoroutineDispatchers,
  private val discovery: RemoteServiceDiscovery,
  private val sharedPreferences: SharedPreferences,
) : ConnectionRepository {
  override suspend fun discover(): DiscoveryStop {
    val discover = discovery.discover()
    if (discover is DiscoveryStop.Complete) {
      save(discover.settings)
    }
    return discover
  }

  override suspend fun save(settings: ConnectionSettings) {
    withContext(dispatchers.database) {
      val entity = settings.toConnectionEntity()

      if (settings.id > 0) {
        dao.update(entity)
      } else {
        dao.insert(entity)
      }

      if (count() == 1L) {
        val id = checkNotNull(dao.last()?.toConnection()).id
        sharedPreferences.edit { putLong("mbrc_default_settings", id) }
      }
    }
  }

  override suspend fun delete(settings: ConnectionSettings) {
    withContext(dispatchers.database) {
      val oldId = settings.id
      dao.delete(settings.toConnectionEntity())
      val defaultId = sharedPreferences.getLong("mbrc_default_settings", -1)
      if (defaultId == oldId && count() > 0) {
        val id = checkNotNull(getItemBefore(oldId) ?: dao.first()?.toConnection()).id
        sharedPreferences.edit { putLong("mbrc_default_settings", id) }
      }
    }
  }

  private fun getItemBefore(id: Long): ConnectionSettings? = dao.getPrevious(id)?.toConnection()

  override fun getDefault(): ConnectionSettings? =
    sharedPreferences
      .getLong("mbrc_default_settings", -1)
      .takeIf { it != -1L }
      ?.let { id ->
        val byId = dao.getById(id)
        byId?.toConnection()?.copy(isDefault = true)
      }

  override fun setDefault(settings: ConnectionSettings) {
    Timber.Forest.d("Setting default to $settings")
    sharedPreferences.edit { putLong("mbrc_default_settings", settings.id) }
  }

  override fun getAll(): Flow<PagingData<ConnectionSettings>> {
    val defaultId = sharedPreferences.getLong("mbrc_default_settings", -1)
    return paged({ dao.getAll() }) {
      if (it.id == defaultId) {
        it.toConnection().copy(isDefault = true)
      } else {
        it.toConnection()
      }
    }
  }

  override suspend fun count(): Long =
    withContext(dispatchers.database) {
      dao.count()
    }
}

fun ConnectionSettingsEntity.toConnection(): ConnectionSettings =
  ConnectionSettings(
    address = address.orEmpty(),
    port = port ?: 3000,
    name = name.orEmpty(),
    isDefault = false,
    id = id ?: 0,
  )

fun ConnectionSettings.toConnectionEntity(): ConnectionSettingsEntity =
  ConnectionSettingsEntity(
    address = address,
    port = port,
    name = name,
    id = if (id > 0) id else null,
  )
