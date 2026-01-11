package com.kelsos.mbrc.core.data.library.album

import androidx.paging.PagingData
import com.kelsos.mbrc.core.common.settings.AlbumSortField
import com.kelsos.mbrc.core.common.settings.SortOrder
import com.kelsos.mbrc.core.data.Repository
import kotlinx.coroutines.flow.Flow

interface AlbumRepository : Repository<Album> {
  fun getAlbumsByArtist(artist: String): Flow<PagingData<Album>>

  fun getAll(field: AlbumSortField, order: SortOrder): Flow<PagingData<Album>>

  fun search(term: String, field: AlbumSortField, order: SortOrder): Flow<PagingData<Album>>

  suspend fun updateCovers(updated: List<AlbumCover>)

  suspend fun getCovers(): List<AlbumCover>

  suspend fun coverCount(): Long
}
