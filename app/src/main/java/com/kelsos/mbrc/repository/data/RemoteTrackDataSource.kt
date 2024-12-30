package com.kelsos.mbrc.repository.data

import com.kelsos.mbrc.constants.Protocol
import com.kelsos.mbrc.data.library.Track
import com.kelsos.mbrc.networking.ApiBase
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class RemoteTrackDataSource
  @Inject
  constructor(
    private val service: ApiBase,
  ) : RemoteDataSource<Track> {
    override suspend fun fetch(): Flow<List<Track>> = service.getAllPages(Protocol.LibraryBrowseTracks, Track::class)
  }
