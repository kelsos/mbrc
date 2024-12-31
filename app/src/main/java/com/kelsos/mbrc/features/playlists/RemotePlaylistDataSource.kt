package com.kelsos.mbrc.features.playlists

import com.kelsos.mbrc.common.data.RemoteDataSource
import com.kelsos.mbrc.networking.ApiBase
import com.kelsos.mbrc.networking.protocol.Protocol
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class RemotePlaylistDataSource
  @Inject
  constructor(
    private val service: ApiBase,
  ) : RemoteDataSource<Playlist> {
    override suspend fun fetch(): Flow<List<Playlist>> = service.getAllPages(Protocol.PLAYLIST_LIST, Playlist::class)
  }
