package com.kelsos.mbrc.features.queue

import com.kelsos.mbrc.common.data.Mapper
import com.kelsos.mbrc.features.library.Album
import com.kelsos.mbrc.features.library.AlbumInfo

class AlbumMapper : Mapper<Album, AlbumInfo> {
  override fun map(from: Album): AlbumInfo = AlbumInfo(from.album!!, from.artist!!, from.cover)
}
