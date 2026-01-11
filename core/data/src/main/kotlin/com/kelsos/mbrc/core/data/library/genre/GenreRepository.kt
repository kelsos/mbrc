package com.kelsos.mbrc.core.data.library.genre

import androidx.paging.PagingData
import com.kelsos.mbrc.core.common.settings.SortOrder
import com.kelsos.mbrc.core.data.Repository
import kotlinx.coroutines.flow.Flow

interface GenreRepository : Repository<Genre> {
  fun getAll(sortOrder: SortOrder): Flow<PagingData<Genre>>
  fun search(term: String, sortOrder: SortOrder): Flow<PagingData<Genre>>
}
