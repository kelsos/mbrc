package com.kelsos.mbrc.features.player

fun TrackInfo.isEmpty(): Boolean = artist.isEmpty() && title.isEmpty() && album.isEmpty() && year.isEmpty() && path.isEmpty()
