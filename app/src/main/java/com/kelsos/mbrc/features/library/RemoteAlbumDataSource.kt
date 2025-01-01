package com.kelsos.mbrc.features.library

import com.kelsos.mbrc.common.data.RemoteDataSource
import com.kelsos.mbrc.networking.ApiBase
import com.kelsos.mbrc.networking.protocol.Protocol
import kotlinx.coroutines.flow.Flow

class RemoteAlbumDataSource(
  private val service: ApiBase,
) : RemoteDataSource<Album> {
  override suspend fun fetch(): Flow<List<Album>> = service.getAllPages(Protocol.LIBRARY_BROWSE_ALBUMS, Album::class)
}
