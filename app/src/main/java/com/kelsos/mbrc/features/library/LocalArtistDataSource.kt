package com.kelsos.mbrc.features.library

import com.kelsos.mbrc.common.data.LocalDataSource
import com.raizlabs.android.dbflow.list.FlowCursorList

interface LocalArtistDataSource : LocalDataSource<Artist> {
  suspend fun getArtistByGenre(genre: String): FlowCursorList<Artist>

  suspend fun getAlbumArtists(): FlowCursorList<Artist>
}
