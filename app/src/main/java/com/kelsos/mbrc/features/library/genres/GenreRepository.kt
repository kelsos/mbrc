package com.kelsos.mbrc.features.library.genres

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

interface GenreRepository : Repository<Genre>

class GenreRepositoryImpl(
  private val api: ApiBase,
  private val dao: GenreDao,
  private val dispatchers: AppCoroutineDispatchers,
) : GenreRepository {
  override suspend fun count(): Long = withContext(dispatchers.database) { dao.count() }

  override fun getAll(): Flow<PagingData<Genre>> =
    paged({ dao.getAll() }) {
      it.toGenre()
    }

  override suspend fun getRemote(progress: Progress?) {
    withContext(dispatchers.network) {
      val added = epoch()
      val stored =
        withContext(dispatchers.database) {
          dao.genres().associate { it.genre to it.id }
        }
      val allPages =
        api.getAllPages(
          Protocol.LibraryBrowseGenres,
          GenreDto::class,
          progress,
        )

      allPages
        .onCompletion {
          withContext(dispatchers.database) {
            dao.removePreviousEntries(added)
          }
        }.collect { genres ->
          val items =
            genres.map {
              val id = stored[it.genre]
              if (id != null) {
                it.toEntity().copy(dateAdded = added, id = id)
              } else {
                it.toEntity().copy(dateAdded = added)
              }
            }
          withContext(dispatchers.database) {
            dao.insertAll(items.filter { it.id == null })
            dao.update(items.filter { it.id != null })
          }
        }
    }
  }

  override fun search(term: String): Flow<PagingData<Genre>> =
    paged({ dao.search(term) }) {
      it.toGenre()
    }

  override suspend fun getById(id: Long): Genre? {
    return withContext(dispatchers.database) {
      val entity = dao.getById(id) ?: return@withContext null
      return@withContext entity.toGenre()
    }
  }
}
