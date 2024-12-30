package com.kelsos.mbrc.repository.data

import com.kelsos.mbrc.constants.Protocol
import com.kelsos.mbrc.data.NowPlaying
import com.kelsos.mbrc.networking.ApiBase
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class RemoteNowPlayingDataSource
@Inject constructor(private val service: ApiBase) : RemoteDataSource<NowPlaying> {
  override suspend fun fetch(): Flow<List<NowPlaying>> {
    return service.getAllPages(Protocol.NowPlayingList, NowPlaying::class)
  }
}
