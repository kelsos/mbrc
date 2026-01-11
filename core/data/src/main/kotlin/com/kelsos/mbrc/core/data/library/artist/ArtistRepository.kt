package com.kelsos.mbrc.core.data.library.artist

import androidx.paging.PagingData
import com.kelsos.mbrc.core.common.settings.SortOrder
import com.kelsos.mbrc.core.data.Repository
import kotlinx.coroutines.flow.Flow

interface ArtistRepository : Repository<Artist> {
  fun getArtistByGenre(genreId: Long, sortOrder: SortOrder): Flow<PagingData<Artist>>

  fun getAll(sortOrder: SortOrder): Flow<PagingData<Artist>>

  fun getAlbumArtistsOnly(sortOrder: SortOrder): Flow<PagingData<Artist>>

  fun search(term: String, sortOrder: SortOrder): Flow<PagingData<Artist>>
}
