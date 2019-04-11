package com.kelsos.mbrc.content.nowplaying

import androidx.paging.PagingData
import arrow.core.Try
import com.kelsos.mbrc.networking.ApiBase
import com.kelsos.mbrc.networking.protocol.Protocol
import com.kelsos.mbrc.utilities.AppCoroutineDispatchers
import com.kelsos.mbrc.utilities.epoch
import com.kelsos.mbrc.utilities.paged
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.withContext

class NowPlayingRepositoryImpl(
  private val api: ApiBase,
  private val dao: NowPlayingDao,
  private val dispatchers: AppCoroutineDispatchers
) : NowPlayingRepository {

  override suspend fun count(): Long = withContext(dispatchers.database) { dao.count() }

  override fun getAll(): Flow<PagingData<NowPlaying>> = dao.getAll().paged {
    it.toNowPlaying()
  }

  override suspend fun getRemote(): Try<Unit> = Try {
    val added = epoch()
    withContext(dispatchers.network) {
      api.getAllPages(Protocol.NowPlayingList, NowPlayingDto::class)
        .onCompletion {
          withContext(dispatchers.database) {
            dao.removePreviousEntries(added)
          }
        }
        .collect { item ->
          val list = item.map { it.toEntity().apply { dateAdded = added } }
          withContext(dispatchers.database) {
            dao.insertAll(list)
          }
        }
    }
  }

  override fun search(
    term: String
  ): Flow<PagingData<NowPlaying>> = dao.search(term).paged { it.toNowPlaying() }

  override suspend fun cacheIsEmpty(): Boolean =
    withContext(dispatchers.database) {
      dao.count() == 0L
    }

  override fun move(from: Int, to: Int) {
    TODO("implement move")
  }
}