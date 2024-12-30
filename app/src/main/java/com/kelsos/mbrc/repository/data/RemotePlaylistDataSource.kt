package com.kelsos.mbrc.repository.data

import com.kelsos.mbrc.constants.Protocol
import com.kelsos.mbrc.data.Playlist
import com.kelsos.mbrc.networking.ApiBase
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject


class RemotePlaylistDataSource
@Inject constructor(private val service: ApiBase) : RemoteDataSource<Playlist> {
  override suspend fun fetch(): Flow<List<Playlist>> {
    return service.getAllPages(Protocol.PlaylistList, Playlist::class)
  }
}
