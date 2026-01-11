package com.kelsos.mbrc.feature.library.albums

import androidx.paging.PagingData
import com.kelsos.mbrc.core.common.data.Progress
import com.kelsos.mbrc.core.common.settings.AlbumSortField
import com.kelsos.mbrc.core.common.settings.SortOrder
import com.kelsos.mbrc.core.common.utilities.coroutines.AppCoroutineDispatchers
import com.kelsos.mbrc.core.common.utilities.epoch
import com.kelsos.mbrc.core.data.library.album.Album
import com.kelsos.mbrc.core.data.library.album.AlbumCover
import com.kelsos.mbrc.core.data.library.album.AlbumDao
import com.kelsos.mbrc.core.data.library.album.AlbumRepository
import com.kelsos.mbrc.core.data.library.album.CachedAlbumCover
import com.kelsos.mbrc.core.data.paged
import com.kelsos.mbrc.core.networking.api.LibraryApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.withContext

class AlbumRepositoryImpl(
  private val dao: AlbumDao,
  private val libraryApi: LibraryApi,
  private val dispatchers: AppCoroutineDispatchers
) : AlbumRepository {
  override suspend fun count(): Long = withContext(dispatchers.database) { dao.count() }

  override fun getAlbumsByArtist(artist: String): Flow<PagingData<Album>> = paged({
    dao.getAlbumsByArtist(artist)
  }) {
    it.toAlbum()
  }

  override fun getAll(): Flow<PagingData<Album>> = getAll(AlbumSortField.NAME, SortOrder.ASC)

  override fun getAll(field: AlbumSortField, order: SortOrder): Flow<PagingData<Album>> = paged({
    when (field) {
      AlbumSortField.NAME -> when (order) {
        SortOrder.ASC -> dao.getAllByNameAsc()
        SortOrder.DESC -> dao.getAllByNameDesc()
      }

      AlbumSortField.ARTIST -> when (order) {
        SortOrder.ASC -> dao.getAllByArtistAsc()
        SortOrder.DESC -> dao.getAllByArtistDesc()
      }

      AlbumSortField.YEAR -> when (order) {
        SortOrder.ASC -> dao.getAllByYearAsc()
        SortOrder.DESC -> dao.getAllByYearDesc()
      }
    }
  }) { it.toAlbum() }

  override suspend fun getRemote(progress: Progress?) {
    withContext(dispatchers.network) {
      val added = epoch()
      val default = CachedAlbumCover(0, null)
      val cached =
        dao.all().associate { entry ->
          entry.album + entry.artist to CachedAlbumCover(entry.id, entry.cover)
        }
      val allPages = libraryApi.getAlbums(progress)

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
                  cover = cachedAlbum.cover
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

  override fun search(term: String): Flow<PagingData<Album>> =
    search(term, AlbumSortField.NAME, SortOrder.ASC)

  override fun search(
    term: String,
    field: AlbumSortField,
    order: SortOrder
  ): Flow<PagingData<Album>> = paged({
    when (field) {
      AlbumSortField.NAME -> when (order) {
        SortOrder.ASC -> dao.searchByNameAsc(term)
        SortOrder.DESC -> dao.searchByNameDesc(term)
      }

      AlbumSortField.ARTIST -> when (order) {
        SortOrder.ASC -> dao.searchByArtistAsc(term)
        SortOrder.DESC -> dao.searchByArtistDesc(term)
      }

      AlbumSortField.YEAR -> when (order) {
        SortOrder.ASC -> dao.searchByYearAsc(term)
        SortOrder.DESC -> dao.searchByYearDesc(term)
      }
    }
  }) { it.toAlbum() }

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
