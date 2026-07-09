package com.kelsos.mbrc.feature.library.domain

import com.kelsos.mbrc.core.common.utilities.epoch
import com.kelsos.mbrc.core.common.utilities.foldForTagIndex
import com.kelsos.mbrc.core.common.utilities.splitTags
import com.kelsos.mbrc.core.data.LibraryTransactionRunner
import com.kelsos.mbrc.core.data.library.album.AlbumDao
import com.kelsos.mbrc.core.data.library.artist.ArtistDao
import com.kelsos.mbrc.core.data.library.artist.ArtistEntity
import com.kelsos.mbrc.core.data.library.genre.GenreDao
import com.kelsos.mbrc.core.data.library.genre.GenreEntity
import com.kelsos.mbrc.core.data.library.junction.TrackArtistDao
import com.kelsos.mbrc.core.data.library.junction.TrackArtistEntity
import com.kelsos.mbrc.core.data.library.junction.TrackGenreDao
import com.kelsos.mbrc.core.data.library.junction.TrackGenreEntity
import com.kelsos.mbrc.core.data.library.track.TrackDao
import com.kelsos.mbrc.feature.library.ui.LibraryMediaType

/**
 * Rebuilds the derived dimension tables (genre, artist, album) and the tag
 * junctions (track_genre, track_artist) from the already-synced track table.
 *
 * MusicBee stores multi-value tags as a single "A; B" string on the track while
 * its browse lists are inconsistent (genres pre-split, artists not). Rather than
 * fetching and reconciling those lists, we treat the track table as the single
 * source of truth: split each tag once and materialize the dimensions plus the
 * many-to-many links. This both fixes multi-value navigation and removes the
 * independently-synced duplicate lists.
 */
class LibraryDerivationUseCase(
  private val trackDao: TrackDao,
  private val genreDao: GenreDao,
  private val artistDao: ArtistDao,
  private val albumDao: AlbumDao,
  private val trackGenreDao: TrackGenreDao,
  private val trackArtistDao: TrackArtistDao,
  private val transactionRunner: LibraryTransactionRunner
) {
  /**
   * Backfills the junctions for an install that already has tracks but no derived
   * links yet (e.g. right after the v4→v5 upgrade, before any sync). No-op once the
   * junctions exist. Network-free, so it can run on library open.
   */
  suspend fun ensureDerived() {
    val needsBackfill = transactionRunner.immediate {
      trackArtistDao.count() == 0L && trackDao.count() > 0L
    }
    if (needsBackfill) {
      derive()
    }
  }

  suspend fun derive(progress: SyncProgress? = null) {
    transactionRunner.immediate {
      val added = epoch()
      val rows = trackDao.tagRows()

      // Dimension rows: insert-or-ignore keeps existing ids (and album covers).
      progress?.invoke(LibraryMediaType.Genres, 0, 1)
      val genreNames = rows.flatMapTo(mutableSetOf()) { splitTags(it.genre) }
      genreDao.insertOrIgnore(genreNames.map { GenreEntity(genre = it, dateAdded = added) })
      val genreIds = genreDao.genres().associate { foldForTagIndex(it.genre) to it.id }
      progress?.invoke(LibraryMediaType.Genres, 1, 1)

      progress?.invoke(LibraryMediaType.Artists, 0, 1)
      val artistNames =
        rows.flatMapTo(mutableSetOf()) {
          splitTags(it.artist) + listOfNotNull(albumArtistName(it.albumArtist))
        }
      artistDao.insertOrIgnore(artistNames.map { ArtistEntity(artist = it, dateAdded = added) })
      val artistIds = artistDao.all().associate { foldForTagIndex(it.artist) to it.id }
      progress?.invoke(LibraryMediaType.Artists, 1, 1)

      progress?.invoke(LibraryMediaType.Albums, 0, 1)
      albumDao.deriveFromTracks(added)
      progress?.invoke(LibraryMediaType.Albums, 1, 1)

      // Junctions: full rebuild from the current track tags.
      trackGenreDao.deleteAll()
      trackArtistDao.deleteAll()

      val trackGenres =
        rows.flatMap { row ->
          splitTags(row.genre).mapNotNull { name ->
            genreIds[foldForTagIndex(name)]?.let { TrackGenreEntity(row.id, it) }
          }
        }
      val trackArtists =
        rows.flatMap { row ->
          val artists = splitTags(row.artist).mapNotNull { name ->
            artistIds[foldForTagIndex(name)]?.let {
              TrackArtistEntity(row.id, it, isAlbumArtist = 0)
            }
          }
          val albumArtists = listOfNotNull(albumArtistName(row.albumArtist)).mapNotNull { name ->
            artistIds[foldForTagIndex(name)]?.let {
              TrackArtistEntity(row.id, it, isAlbumArtist = 1)
            }
          }
          artists + albumArtists
        }

      trackGenres.chunked(INSERT_CHUNK).forEach { trackGenreDao.insertAll(it) }
      trackArtists.chunked(INSERT_CHUNK).forEach { trackArtistDao.insertAll(it) }

      // Drop dimensions that no longer back any track.
      genreDao.removeOrphans()
      artistDao.removeOrphans()
      albumDao.removeOrphans()
    }
  }

  /**
   * Album artist is a single-value field in MusicBee (the multi-value splitter
   * covers artist/performer/guest artist/remixer, never album artist), so it is
   * taken verbatim - a ';' in it is bad tag data, not a separator.
   */
  private fun albumArtistName(raw: String): String? = raw.trim().ifEmpty { null }

  private companion object {
    const val INSERT_CHUNK = 2000
  }
}
