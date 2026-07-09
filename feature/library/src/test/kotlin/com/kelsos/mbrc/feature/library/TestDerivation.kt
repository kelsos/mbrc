package com.kelsos.mbrc.feature.library

import com.kelsos.mbrc.core.data.Database
import com.kelsos.mbrc.core.data.LibraryTransactionRunner
import com.kelsos.mbrc.feature.library.domain.LibraryDerivationUseCase

/**
 * Test helper: rebuilds the derived dimension tables and tag junctions from the
 * currently-seeded tracks, mirroring what a real sync does. Repository navigation
 * tests seed tracks then call this before querying genre/artist/album relations.
 */
suspend fun Database.deriveLibrary() {
  LibraryDerivationUseCase(
    trackDao = trackDao(),
    genreDao = genreDao(),
    artistDao = artistDao(),
    albumDao = albumDao(),
    trackGenreDao = trackGenreDao(),
    trackArtistDao = trackArtistDao(),
    transactionRunner = LibraryTransactionRunner(this)
  ).derive()
}
