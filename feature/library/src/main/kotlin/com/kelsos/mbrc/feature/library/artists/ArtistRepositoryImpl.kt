package com.kelsos.mbrc.feature.library.artists

import androidx.paging.PagingData
import com.kelsos.mbrc.core.common.data.Progress
import com.kelsos.mbrc.core.common.settings.SortOrder
import com.kelsos.mbrc.core.common.utilities.coroutines.AppCoroutineDispatchers
import com.kelsos.mbrc.core.common.utilities.epoch
import com.kelsos.mbrc.core.data.library.artist.Artist
import com.kelsos.mbrc.core.data.library.artist.ArtistDao
import com.kelsos.mbrc.core.data.library.artist.ArtistRepository
import com.kelsos.mbrc.core.data.paged
import com.kelsos.mbrc.core.networking.api.LibraryApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.withContext

class ArtistRepositoryImpl(
  private val dao: ArtistDao,
  private val libraryApi: LibraryApi,
  private val dispatchers: AppCoroutineDispatchers
) : ArtistRepository {
  override suspend fun count(): Long = withContext(dispatchers.database) {
    dao.count()
  }

  override fun getArtistByGenre(genreId: Long, sortOrder: SortOrder): Flow<PagingData<Artist>> =
    paged({
      when (sortOrder) {
        SortOrder.ASC -> dao.getArtistByGenreAsc(genreId)
        SortOrder.DESC -> dao.getArtistByGenreDesc(genreId)
      }
    }) {
      it.toArtist()
    }

  override fun getAll(): Flow<PagingData<Artist>> = getAll(SortOrder.ASC)

  override fun getAll(sortOrder: SortOrder): Flow<PagingData<Artist>> = paged({
    when (sortOrder) {
      SortOrder.ASC -> dao.getAllAsc()
      SortOrder.DESC -> dao.getAllDesc()
    }
  }) { it.toArtist() }

  override suspend fun getRemote(progress: Progress?) {
    withContext(dispatchers.network) {
      val added = epoch()
      val allPages = libraryApi.getArtists(progress)

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

  override fun search(term: String): Flow<PagingData<Artist>> = search(term, SortOrder.ASC)

  override fun search(term: String, sortOrder: SortOrder): Flow<PagingData<Artist>> = paged({
    when (sortOrder) {
      SortOrder.ASC -> dao.searchAsc(term)
      SortOrder.DESC -> dao.searchDesc(term)
    }
  }) { it.toArtist() }

  override fun getAlbumArtistsOnly(sortOrder: SortOrder): Flow<PagingData<Artist>> = paged({
    when (sortOrder) {
      SortOrder.ASC -> dao.getAlbumArtistsAsc()
      SortOrder.DESC -> dao.getAlbumArtistsDesc()
    }
  }) { it.toArtist() }

  override suspend fun getById(id: Long): Artist? {
    return withContext(dispatchers.database) {
      val entity = dao.getById(id) ?: return@withContext null
      return@withContext entity.toArtist()
    }
  }
}
