package com.kelsos.mbrc.features.library

import androidx.annotation.StringRes
import com.kelsos.mbrc.R

sealed class LibraryMediaType(
  val code: Int,
  @StringRes val nameRes: Int = 0,
) {
  object Genres : LibraryMediaType(GENRES, R.string.media__genres)

  object Artists : LibraryMediaType(ARTISTS, R.string.media__artists)

  object Albums : LibraryMediaType(ALBUMS, R.string.media__albums)

  object Tracks : LibraryMediaType(TRACKS, R.string.media__tracks)

  object Playlists : LibraryMediaType(PLAYLISTS, R.string.media__playlists)

  object Covers : LibraryMediaType(COVERS, R.string.media__covers)

  companion object {
    const val GENRES = 1
    const val ARTISTS = 2
    const val ALBUMS = 3
    const val TRACKS = 4
    const val PLAYLISTS = 5
    const val COVERS = 6

    fun fromCode(code: Int): LibraryMediaType =
      when (code) {
        GENRES -> Genres
        ARTISTS -> Artists
        ALBUMS -> Albums
        TRACKS -> Tracks
        PLAYLISTS -> Playlists
        COVERS -> Covers
        else -> throw IllegalArgumentException("Unknown media type code: $code")
      }
  }
}
