package com.kelsos.mbrc.feature.settings.domain

import androidx.paging.PagingData
import com.kelsos.mbrc.core.common.data.ConnectionSettings
import com.kelsos.mbrc.core.common.utilities.coroutines.AppCoroutineDispatchers
import com.kelsos.mbrc.core.data.paged
import com.kelsos.mbrc.core.data.settings.ConnectionDao
import com.kelsos.mbrc.core.data.settings.ConnectionSettingsEntity
import com.kelsos.mbrc.core.networking.discovery.DiscoveryStop
import com.kelsos.mbrc.core.networking.discovery.RemoteServiceDiscovery
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import timber.log.Timber

interface ConnectionRepository {
  suspend fun save(settings: ConnectionSettings)

  /**
   * The saved host that is the computer behind a connect link: stored at one of [addresses] on
   * [port] under [name] (ignoring case), the first such in the order given.
   *
   * The name has to agree because an address alone does not identify a computer across networks:
   * VirtualBox, Docker and VPN adapters reuse the same addresses on every machine, and two homes can
   * share a subnet. A host the user renamed is therefore not matched here; it is still reused when
   * the address that answers is the one it is saved at.
   */
  suspend fun findSaved(addresses: List<String>, port: Int, name: String?): ConnectionSettings?

  /**
   * Makes the host at [address] and [port] the default and returns it as stored. An entry already
   * at [address] is reused; failing that the entry [sameHostId] (found by [findSaved]) is moved to
   * [address]; only when neither applies is a new entry added under [name]. A reused or moved entry
   * keeps its own name.
   */
  suspend fun saveAsDefault(
    address: String,
    port: Int,
    name: String,
    sameHostId: Long?
  ): ConnectionSettings

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
  private val discovery: RemoteServiceDiscovery
) : ConnectionRepository {
  override suspend fun discover(): DiscoveryStop {
    val discover = discovery.discover()
    if (discover is DiscoveryStop.Complete) {
      discover.settings.forEach { host ->
        // Skip hosts already saved with the same (address, port) — repeated
        // scans should not produce duplicate rows. Any other save failure
        // is logged so one bad host doesn't drop the rest of the batch.
        val existingId = withContext(dispatchers.database) {
          dao.findId(host.address, host.port)
        }
        if (existingId != null) {
          Timber.v(
            "Discovered host %s:%d already saved (id=%d)",
            host.address,
            host.port,
            existingId
          )
          return@forEach
        }
        try {
          save(host)
        } catch (e: CancellationException) {
          throw e
        } catch (e: Exception) {
          Timber.w(e, "Failed to save discovered host %s:%d", host.address, host.port)
        }
      }
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
        dao.updateDefault(checkNotNull(dao.last()?.toConnection()).id)
      }
    }
  }

  override suspend fun findSaved(
    addresses: List<String>,
    port: Int,
    name: String?
  ): ConnectionSettings? {
    if (name.isNullOrBlank()) {
      return null
    }
    return withContext(dispatchers.database) {
      val sameName = dao.findAll(addresses, port).filter { it.name.equals(name, ignoreCase = true) }
      addresses.firstNotNullOfOrNull { address -> sameName.find { it.address == address } }
        ?.toConnection()
    }
  }

  override suspend fun saveAsDefault(
    address: String,
    port: Int,
    name: String,
    sameHostId: Long?
  ): ConnectionSettings = withContext(dispatchers.database) {
    val id = dao.saveAsDefault(address, port, name, sameHostId)
    checkNotNull(dao.getById(id)).toConnection()
  }

  override suspend fun delete(settings: ConnectionSettings) {
    withContext(dispatchers.database) {
      val oldId = settings.id
      dao.delete(settings.toConnectionEntity())
      val default = dao.getDefault()
      if (default == null && count() > 0) {
        dao.updateDefault(checkNotNull(getItemBefore(oldId) ?: dao.first()?.toConnection()).id)
      }
    }
  }

  private fun getItemBefore(id: Long): ConnectionSettings? = dao.getPrevious(id)?.toConnection()

  override fun getDefault(): ConnectionSettings? = dao.getDefault()?.toConnection()

  override fun setDefault(settings: ConnectionSettings) {
    dao.updateDefault(settings.id)
  }

  override fun getAll(): Flow<PagingData<ConnectionSettings>> =
    paged({ dao.getAll() }, key = { it.id }) {
      it.toConnection()
    }

  override suspend fun count(): Long = withContext(dispatchers.database) {
    dao.count()
  }
}

fun ConnectionSettingsEntity.toConnection(): ConnectionSettings = ConnectionSettings(
  address = address,
  port = port,
  name = name,
  isDefault = isDefault ?: false,
  id = id
)

fun ConnectionSettings.toConnectionEntity(): ConnectionSettingsEntity = ConnectionSettingsEntity(
  address = address,
  port = port,
  name = name,
  isDefault = isDefault,
  id = id
)
