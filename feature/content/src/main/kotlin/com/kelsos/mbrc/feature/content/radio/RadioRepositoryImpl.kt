package com.kelsos.mbrc.feature.content.radio

import androidx.paging.PagingData
import com.kelsos.mbrc.core.common.data.Progress
import com.kelsos.mbrc.core.common.utilities.coroutines.AppCoroutineDispatchers
import com.kelsos.mbrc.core.common.utilities.epoch
import com.kelsos.mbrc.core.data.paged
import com.kelsos.mbrc.core.data.radio.RadioRepository
import com.kelsos.mbrc.core.data.radio.RadioStation
import com.kelsos.mbrc.core.data.radio.RadioStationDao
import com.kelsos.mbrc.core.networking.api.ContentApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.withContext

class RadioRepositoryImpl(
  private val dao: RadioStationDao,
  private val contentApi: ContentApi,
  private val dispatchers: AppCoroutineDispatchers
) : RadioRepository {
  override suspend fun count(): Long = withContext(dispatchers.database) {
    dao.count()
  }

  override fun getAll(): Flow<PagingData<RadioStation>> = paged({
    dao.getAll()
  }, key = { it.id }) { it.toRadioStation() }

  override suspend fun getRemote(progress: Progress?) {
    withContext(dispatchers.network) {
      val added = epoch()
      val allPages = contentApi.getRadioStations(progress)
      allPages
        .onCompletion {
          withContext(dispatchers.database) {
            dao.removePreviousEntries(added)
          }
        }.collect { radios ->
          val data = radios.map { it.toEntity().copy(dateAdded = added) }
          withContext(dispatchers.database) {
            dao.insertAll(data)
          }
        }
    }
  }

  override fun search(term: String): Flow<PagingData<RadioStation>> = paged({
    dao.search(term)
  }, key = { it.id }) {
    it.toRadioStation()
  }

  override suspend fun getById(id: Long): RadioStation? {
    return withContext(dispatchers.database) {
      val entity = dao.getById(id) ?: return@withContext null
      return@withContext entity.toRadioStation()
    }
  }
}
