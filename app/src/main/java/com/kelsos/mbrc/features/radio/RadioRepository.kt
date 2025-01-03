package com.kelsos.mbrc.features.radio

import androidx.paging.PagingData
import com.kelsos.mbrc.common.data.Progress
import com.kelsos.mbrc.common.data.Repository
import com.kelsos.mbrc.common.utilities.AppCoroutineDispatchers
import com.kelsos.mbrc.common.utilities.epoch
import com.kelsos.mbrc.common.utilities.paged
import com.kelsos.mbrc.networking.ApiBase
import com.kelsos.mbrc.networking.protocol.Protocol
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.withContext

interface RadioRepository : Repository<RadioStation>

class RadioRepositoryImpl(
  private val dao: RadioStationDao,
  private val api: ApiBase,
  private val dispatchers: AppCoroutineDispatchers,
) : RadioRepository {
  override suspend fun count(): Long =
    withContext(dispatchers.database) {
      dao.count()
    }

  override fun getAll(): Flow<PagingData<RadioStation>> = paged({ dao.getAll() }) { it.toRadioStation() }

  override suspend fun getRemote(progress: Progress?) {
    withContext(dispatchers.network) {
      val added = epoch()
      val allPages =
        api.getAllPages(
          Protocol.RadioStations,
          RadioStationDto::class,
          progress,
        )
      allPages
        .onCompletion {
          withContext(dispatchers.database) {
            dao.removePreviousEntries(added)
          }
        }.collect { radios ->
          val data = radios.map { it.toEntity().copy(dateAdded = added) }
          withContext(dispatchers.database) {
            withContext(dispatchers.database) {
              dao.insertAll(data)
            }
          }
        }
    }
  }

  override fun search(term: String): Flow<PagingData<RadioStation>> =
    paged({
      dao.search(term)
    }) {
      it.toRadioStation()
    }

  override suspend fun getById(id: Long): RadioStation? {
    return withContext(dispatchers.database) {
      val entity = dao.getById(id) ?: return@withContext null
      return@withContext entity.toRadioStation()
    }
  }
}
