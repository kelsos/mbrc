package com.kelsos.mbrc.repository

import com.kelsos.mbrc.data.library.Artist
import com.raizlabs.android.dbflow.list.FlowCursorList

interface ArtistRepository : Repository<Artist> {
  suspend fun getArtistByGenre(genre: String): FlowCursorList<Artist>

  suspend fun getAlbumArtistsOnly(): FlowCursorList<Artist>

  suspend fun getAllRemoteAndShowAlbumArtist(): FlowCursorList<Artist>
}
