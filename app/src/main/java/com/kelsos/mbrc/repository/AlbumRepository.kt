package com.kelsos.mbrc.repository

import com.kelsos.mbrc.data.CoverInfo
import com.kelsos.mbrc.data.library.Album
import com.raizlabs.android.dbflow.list.FlowCursorList

interface AlbumRepository : Repository<Album> {
  suspend fun getAlbumsByArtist(artist: String): FlowCursorList<Album>
  suspend fun updateCovers(updated: List<CoverInfo>)
}
