package com.kelsos.mbrc.features.nowplaying

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
import timber.log.Timber

interface NowPlayingRepository : Repository<NowPlaying> {
  suspend fun move(
    from: Int,
    to: Int,
  )

  suspend fun remove(position: Int)

  suspend fun findPosition(query: String): Int
}

val NowPlayingEntity.key: String
  get() = "$path-$position"

val CachedNowPlaying.key: String
  get() = "$path-$position"

class NowPlayingRepositoryImpl(
  private val api: ApiBase,
  private val dao: NowPlayingDao,
  private val dispatchers: AppCoroutineDispatchers,
) : NowPlayingRepository {
  override suspend fun count(): Long = withContext(dispatchers.database) { dao.count() }

  override fun getAll(): Flow<PagingData<NowPlaying>> =
    paged({ dao.getAll() }) {
      it.toNowPlaying()
    }

  override suspend fun getRemote(progress: Progress?) {
    withContext(dispatchers.network) {
      val added = epoch()
      val cached =
        withContext(dispatchers.database) {
          dao.cached().associateBy { it.key }
        }
      api
        .getAllPages(
          Protocol.NowPlayingList,
          NowPlayingDto::class,
          progress,
        ).onCompletion {
          withContext(dispatchers.database) {
            dao.removePreviousEntries(added)
          }
        }.collect { item ->
          val list = item.map { it.toEntity().copy(dateAdded = added) }

          val existing = list.filter { cached.containsKey(it.key) }
          val new = list.minus(existing.toSet())
          val update = existing.map { entity -> entity.copy(id = checkNotNull(cached[entity.key]).id) }

          withContext(dispatchers.database) {
            Timber.v("updating ${update.size} and inserting ${new.size} items")
            dao.update(update)
            dao.insertAll(new)
          }
        }
    }
  }

  override fun search(term: String): Flow<PagingData<NowPlaying>> = paged({ dao.search(term) }) { it.toNowPlaying() }

  override suspend fun move(
    from: Int,
    to: Int,
  ) {
    withContext(dispatchers.database) {
      dao.move(from, to)
    }
  }

  override suspend fun remove(position: Int) {
    withContext(dispatchers.database) {
      dao.remove(position)
    }
  }

  override suspend fun findPosition(query: String): Int =
    withContext(dispatchers.database) {
      return@withContext dao.findPositionByQuery(query) ?: -1
    }

  override suspend fun getById(id: Long): NowPlaying? {
    return withContext(dispatchers.database) {
      val entity = dao.getById(id) ?: return@withContext null
      return@withContext entity.toNowPlaying()
    }
  }
}
