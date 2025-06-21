package com.kelsos.mbrc.features.library

import androidx.work.Data
import androidx.work.workDataOf

data class LibraryStats(
  val genres: Long,
  val artists: Long,
  val albums: Long,
  val tracks: Long,
  val playlists: Long,
  val covers: Long,
)

fun LibraryStats.toWorkData(): Data =
  workDataOf(
    "genres" to genres,
    "artists" to artists,
    "albums" to albums,
    "tracks" to tracks,
    "playlists" to playlists,
    "covers" to covers,
  )

fun Data.toLibraryStats(): LibraryStats =
  LibraryStats(
    getLong("genres", 0),
    getLong("artists", 0),
    getLong("albums", 0),
    getLong("tracks", 0),
    getLong("playlists", 0),
    getLong("covers", 0),
  )
