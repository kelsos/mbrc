package com.kelsos.mbrc.features.library

import com.kelsos.mbrc.common.data.RemoteDataSource
import com.kelsos.mbrc.networking.ApiBase
import com.kelsos.mbrc.networking.protocol.Protocol
import kotlinx.coroutines.flow.Flow

class RemoteGenreDataSource(
  private val service: ApiBase,
) : RemoteDataSource<Genre> {
  override suspend fun fetch(): Flow<List<Genre>> = service.getAllPages(Protocol.LIBRARY_BROWSE_GENRES, Genre::class)
}
