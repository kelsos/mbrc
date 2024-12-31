package com.kelsos.mbrc.features.library

import com.kelsos.mbrc.common.data.RemoteDataSource
import com.kelsos.mbrc.networking.ApiBase
import com.kelsos.mbrc.networking.protocol.Protocol
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class RemoteTrackDataSource
  @Inject
  constructor(
    private val service: ApiBase,
  ) : RemoteDataSource<Track> {
    override suspend fun fetch(): Flow<List<Track>> = service.getAllPages(Protocol.LIBRARY_BROWSE_TRACKS, Track::class)
  }
