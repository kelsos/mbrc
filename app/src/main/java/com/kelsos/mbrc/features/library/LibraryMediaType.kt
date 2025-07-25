package com.kelsos.mbrc.features.library

import androidx.annotation.StringRes
import com.kelsos.mbrc.R

sealed class LibraryMediaType(val code: Int, @StringRes val progressRes: Int = 0) {
  object Genres : LibraryMediaType(GENRES, R.string.notification__sync_progress_genres)

  object Artists : LibraryMediaType(ARTISTS, R.string.notification__sync_progress_artists)

  object Albums : LibraryMediaType(ALBUMS, R.string.notification__sync_progress_albums)

  object Tracks : LibraryMediaType(TRACKS, R.string.notification__sync_progress_tracks)

  object Playlists : LibraryMediaType(PLAYLISTS, R.string.notification__sync_progress_playlists)

  object Covers : LibraryMediaType(COVERS, R.string.notification__sync_progress_covers)

  companion object {
    const val GENRES = 1
    const val ARTISTS = 2
    const val ALBUMS = 3
    const val TRACKS = 4
    const val PLAYLISTS = 5
    const val COVERS = 6

    fun fromCode(code: Int): LibraryMediaType = when (code) {
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
