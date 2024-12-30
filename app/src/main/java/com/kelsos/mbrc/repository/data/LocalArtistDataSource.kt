package com.kelsos.mbrc.repository.data

import com.kelsos.mbrc.data.library.Artist
import com.raizlabs.android.dbflow.list.FlowCursorList

interface LocalArtistDataSource : LocalDataSource<Artist> {
  suspend fun getArtistByGenre(genre: String): FlowCursorList<Artist>
  suspend fun getAlbumArtists(): FlowCursorList<Artist>
}
