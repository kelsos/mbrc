package com.kelsos.mbrc.features.nowplaying

import com.kelsos.mbrc.common.data.RemoteDataSource
import com.kelsos.mbrc.networking.ApiBase
import com.kelsos.mbrc.networking.protocol.Protocol
import kotlinx.coroutines.flow.Flow

class RemoteNowPlayingDataSource(
  private val service: ApiBase,
) : RemoteDataSource<NowPlaying> {
  override suspend fun fetch(): Flow<List<NowPlaying>> = service.getAllPages(Protocol.NOW_PLAYING_LIST, NowPlaying::class)
}
