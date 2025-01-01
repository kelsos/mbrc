package com.kelsos.mbrc.features.library

import com.kelsos.mbrc.common.data.RemoteDataSource
import com.kelsos.mbrc.networking.ApiBase
import com.kelsos.mbrc.networking.protocol.Protocol
import kotlinx.coroutines.flow.Flow

class RemoteArtistDataSource(
  private val service: ApiBase,
) : RemoteDataSource<Artist> {
  override suspend fun fetch(): Flow<List<Artist>> = service.getAllPages(Protocol.LIBRARY_BROWSE_ARTISTS, Artist::class)
}
