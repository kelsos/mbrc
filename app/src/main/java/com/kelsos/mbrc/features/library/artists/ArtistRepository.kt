package com.kelsos.mbrc.features.library.artists

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

interface ArtistRepository : Repository<Artist> {
  fun getArtistByGenre(genreId: Long): Flow<PagingData<Artist>>

  fun getAlbumArtistsOnly(): Flow<PagingData<Artist>>
}

class ArtistRepositoryImpl(
  private val dao: ArtistDao,
  private val api: ApiBase,
  private val dispatchers: AppCoroutineDispatchers
) : ArtistRepository {
  override suspend fun count(): Long = withContext(dispatchers.database) {
    dao.count()
  }

  override fun getArtistByGenre(genreId: Long): Flow<PagingData<Artist>> = paged({
    dao.getArtistByGenre(genreId)
  }) {
    it.toArtist()
  }

  override fun getAll(): Flow<PagingData<Artist>> = paged({ dao.getAll() }) {
    it.toArtist()
  }

  override suspend fun getRemote(progress: Progress?) {
    withContext(dispatchers.network) {
      val added = epoch()
      val allPages =
        api.getAllPages(
          Protocol.LibraryBrowseArtists,
          ArtistDto::class,
          progress
        )

      allPages
        .onCompletion {
          withContext(dispatchers.database) {
            dao.removePreviousEntries(added)
          }
        }.collect { artists ->
          val data = artists.map { it.toEntity().copy(dateAdded = added) }
          withContext(dispatchers.database) {
            dao.insertAll(data)
          }
        }
    }
  }

  override fun search(term: String): Flow<PagingData<Artist>> = paged({
    dao.search(term)
  }) { it.toArtist() }

  override fun getAlbumArtistsOnly(): Flow<PagingData<Artist>> = paged({
    dao.getAlbumArtists()
  }) { it.toArtist() }

  override suspend fun getById(id: Long): Artist? {
    return withContext(dispatchers.database) {
      val entity = dao.getById(id) ?: return@withContext null
      return@withContext entity.toArtist()
    }
  }
}
