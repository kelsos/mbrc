package com.kelsos.mbrc.features.radio.repository

import androidx.paging.DataSource
import arrow.core.Either
import com.kelsos.mbrc.common.data.Progress
import com.kelsos.mbrc.common.utilities.AppDispatchers
import com.kelsos.mbrc.common.utilities.epoch
import com.kelsos.mbrc.features.radio.RadioDaoMapper
import com.kelsos.mbrc.features.radio.RadioDtoMapper
import com.kelsos.mbrc.features.radio.RadioStationDto
import com.kelsos.mbrc.features.radio.data.RadioStationDao
import com.kelsos.mbrc.features.radio.domain.RadioStation
import com.kelsos.mbrc.networking.ApiBase
import com.kelsos.mbrc.networking.protocol.Protocol
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.withContext

class RadioRepositoryImpl(
  private val dao: RadioStationDao,
  private val remoteDataSource: ApiBase,
  private val dispatchers: AppDispatchers
) : RadioRepository {

  override suspend fun count(): Long {
    return withContext(dispatchers.database) { dao.count() }
  }

  override fun getAll(): DataSource.Factory<Int, RadioStation> {
    return dao.getAll().map { RadioDaoMapper.map(it) }
  }

  override suspend fun getRemote(progress: Progress): Either<Throwable, Unit> = Either.catch {
    val added = epoch()
    val pages = remoteDataSource.getAllPages(
      Protocol.RadioStations,
      RadioStationDto::class,
      progress
    )
    pages.collect { page ->
      withContext(dispatchers.io) {
        val items = page.map { RadioDtoMapper.map(it).apply { dateAdded = added } }

        withContext(dispatchers.database) {
          dao.insertAll(items)
        }
      }
    }

    withContext(dispatchers.database) {
      dao.removePreviousEntries(added)
    }
  }

  override fun search(term: String): DataSource.Factory<Int, RadioStation> {
    return dao.search(term).map { RadioDaoMapper.map(it) }
  }

  override suspend fun cacheIsEmpty(): Boolean {
    return withContext(dispatchers.database) { dao.count() == 0L }
  }

  override suspend fun getById(id: Long): RadioStation? {
    return withContext(dispatchers.database) {
      val entity = dao.getById(id) ?: return@withContext null
      return@withContext RadioDaoMapper.map(entity)
    }
  }
}
