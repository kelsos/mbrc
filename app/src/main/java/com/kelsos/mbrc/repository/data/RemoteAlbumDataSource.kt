package com.kelsos.mbrc.repository.data

import com.kelsos.mbrc.constants.Protocol
import com.kelsos.mbrc.data.library.Album
import com.kelsos.mbrc.networking.ApiBase
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class RemoteAlbumDataSource
  @Inject
  constructor(
    private val service: ApiBase,
  ) : RemoteDataSource<Album> {
    override suspend fun fetch(): Flow<List<Album>> = service.getAllPages(Protocol.LibraryBrowseAlbums, Album::class)
  }
