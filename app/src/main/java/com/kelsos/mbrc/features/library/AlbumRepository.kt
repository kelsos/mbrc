package com.kelsos.mbrc.features.library

import com.kelsos.mbrc.common.data.Repository
import com.raizlabs.android.dbflow.list.FlowCursorList

interface AlbumRepository : Repository<Album> {
  suspend fun getAlbumsByArtist(artist: String): FlowCursorList<Album>

  suspend fun updateCovers(updated: List<CoverInfo>)
}
