package com.kelsos.mbrc.repository.data

import com.kelsos.mbrc.constants.Protocol
import com.kelsos.mbrc.data.library.Genre
import com.kelsos.mbrc.networking.ApiBase
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class RemoteGenreDataSource
  @Inject
  constructor(
    private val service: ApiBase,
  ) : RemoteDataSource<Genre> {
    override suspend fun fetch(): Flow<List<Genre>> = service.getAllPages(Protocol.LibraryBrowseGenres, Genre::class)
  }
