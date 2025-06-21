package com.kelsos.mbrc.features.library.albums

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

interface AlbumRepository : Repository<Album> {
  fun getAlbumsByArtist(artist: String): Flow<PagingData<Album>>

  suspend fun updateCovers(updated: List<AlbumCover>)

  suspend fun getCovers(): List<AlbumCover>

  suspend fun coverCount(): Long
}

class AlbumRepositoryImpl(
  private val dao: AlbumDao,
  private val api: ApiBase,
  private val dispatchers: AppCoroutineDispatchers,
) : AlbumRepository {
  override suspend fun count(): Long = withContext(dispatchers.database) { dao.count() }

  override fun getAlbumsByArtist(artist: String): Flow<PagingData<Album>> =
    paged({
      dao.getAlbumsByArtist(artist)
    }) {
      it.toAlbum()
    }

  override fun getAll(): Flow<PagingData<Album>> = paged({ dao.getAll() }) { it.toAlbum() }

  override suspend fun getRemote(progress: Progress?) {
    withContext(dispatchers.network) {
      val added = epoch()
      val default = CachedAlbumCover(0, null)
      val cached =
        dao.all().associate { entry ->
          entry.album + entry.artist to CachedAlbumCover(entry.id ?: 0, entry.cover)
        }
      val allPages =
        api.getAllPages(
          Protocol.LibraryBrowseAlbums,
          AlbumDto::class,
          progress,
        )

      allPages
        .onCompletion {
          withContext(dispatchers.database) {
            dao.removePreviousEntries(added)
          }
        }.collect { albums ->
          val list =
            albums.map { dto ->
              val key = dto.album + dto.artist

              if (cached.containsKey(key)) {
                val cachedAlbum = cached.getOrDefault(key, default)
                dto.toEntity().copy(
                  dateAdded = added,
                  id = cachedAlbum.id,
                  cover = cachedAlbum.cover,
                )
              } else {
                dto.toEntity().copy(dateAdded = added)
              }
            }
          withContext(dispatchers.database) {
            dao.insert(list)
          }
        }
    }
  }

  override fun search(term: String): Flow<PagingData<Album>> = paged({ dao.search(term) }) { it.toAlbum() }

  override suspend fun updateCovers(updated: List<AlbumCover>) {
    dao.updateCovers(updated)
  }

  override suspend fun getCovers(): List<AlbumCover> = dao.getCovers()

  override suspend fun getById(id: Long): Album? {
    return withContext(dispatchers.database) {
      val entity = dao.getById(id) ?: return@withContext null
      return@withContext entity.toAlbum()
    }
  }

  override suspend fun coverCount(): Long = withContext(dispatchers.database) { dao.coverCount() }
}
