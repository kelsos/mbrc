package com.kelsos.mbrc.features.library

import com.kelsos.mbrc.features.library.albums.AlbumCover
import okio.ByteString.Companion.encodeUtf8

fun AlbumCover.key(): String = "${artist}_$album".encodeUtf8().sha1().hex().uppercase()
