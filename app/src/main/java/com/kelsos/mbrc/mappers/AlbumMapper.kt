package com.kelsos.mbrc.mappers

import com.kelsos.mbrc.data.library.Album
import com.kelsos.mbrc.domain.AlbumInfo

class AlbumMapper : Mapper<Album, AlbumInfo> {
  override fun map(from: Album): AlbumInfo {
    return AlbumInfo(from.album!!, from.artist!!, from.cover)
  }
}
