package com.kelsos.mbrc.repository.data

import com.kelsos.mbrc.constants.Protocol
import com.kelsos.mbrc.data.library.Artist
import com.kelsos.mbrc.networking.ApiBase
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class RemoteArtistDataSource
  @Inject
  constructor(
    private val service: ApiBase,
  ) : RemoteDataSource<Artist> {
    override suspend fun fetch(): Flow<List<Artist>> = service.getAllPages(Protocol.LibraryBrowseArtists, Artist::class)
  }
