package com.kelsos.mbrc.features.library.repositories

import androidx.paging.PagingData
import com.kelsos.mbrc.common.data.Repository
import com.kelsos.mbrc.features.library.data.Album
import com.kelsos.mbrc.features.library.data.AlbumCover
import kotlinx.coroutines.flow.Flow

interface AlbumRepository : Repository<Album> {
  fun getAlbumsByArtist(artist: String): Flow<PagingData<Album>>
  suspend fun updateCovers(updated: List<AlbumCover>)
  suspend fun getCovers(): List<AlbumCover>
}
