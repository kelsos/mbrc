package com.kelsos.mbrc.feature.library.domain

import android.database.SQLException
import com.kelsos.mbrc.core.common.utilities.AppError
import com.kelsos.mbrc.core.common.utilities.Outcome
import com.kelsos.mbrc.core.data.library.album.AlbumRepository
import com.kelsos.mbrc.core.data.library.artist.ArtistRepository
import com.kelsos.mbrc.core.data.library.genre.GenreRepository
import com.kelsos.mbrc.core.data.library.track.TrackRepository
import com.kelsos.mbrc.core.data.playlist.PlaylistRepository
import com.kelsos.mbrc.feature.library.data.CoverCache
import com.kelsos.mbrc.feature.library.data.LibraryStats
import com.kelsos.mbrc.feature.library.ui.LibraryMediaType
import okio.IOException
import timber.log.Timber

typealias SyncProgress = suspend (category: LibraryMediaType, current: Int, total: Int) -> Unit
typealias SyncOutcome = Outcome<LibraryStats>

/**
 * The class is responsible for the library metadata and playlist data sync.
 */
interface LibrarySyncUseCase {
  /**
   * Starts the sync process for the library and playlist metadata. The sync can be
   * either manual or automatic. The automatic sync should happen only under certain
   * conditions.
   *
   * @param auto Marks the sync process as automatic (initiated by conditions) or
   * manual (initiated by the user)
   */
  suspend fun sync(auto: Boolean = false, progress: SyncProgress? = null): SyncOutcome

  /**
   * Ensures the derived tag junctions exist for an already-synced library (used to
   * backfill after the schema upgrade). Network-free and a no-op once present.
   */
  suspend fun ensureDerived()

  /**
   * Provides access to the interactor's current status.
   *
   * @return Should return true if the interactor is active and running, or false if not
   */
  fun isRunning(): Boolean

  suspend fun syncStats(): LibraryStats
}

class LibrarySyncUseCaseImpl(
  private val genreRepository: GenreRepository,
  private val artistRepository: ArtistRepository,
  private val albumRepository: AlbumRepository,
  private val trackRepository: TrackRepository,
  private val playlistRepository: PlaylistRepository,
  private val libraryDerivationUseCase: LibraryDerivationUseCase,
  private val coverCache: CoverCache
) : LibrarySyncUseCase {
  private var running: Boolean = false

  override suspend fun sync(auto: Boolean, progress: SyncProgress?): SyncOutcome {
    if (isRunning()) {
      Timber.v("Sync is already running")
      return Outcome.Failure(AppError.NoOp)
    }

    running = true
    Timber.v("Starting library metadata sync")
    val shouldSync = checkIfShouldSync(auto)
    if (!shouldSync) {
      running = false
      return Outcome.Failure(AppError.NoOp)
    }

    try {
      // Tracks are the single source of truth; genres, artists and albums are
      // derived from them (see LibraryDerivationUseCase) instead of being fetched
      // and reconciled separately.
      trackRepository.getRemote { current, total ->
        progress?.invoke(LibraryMediaType.Tracks, current, total)
      }
      libraryDerivationUseCase.derive(progress)
      playlistRepository.getRemote { current, total ->
        progress?.invoke(LibraryMediaType.Playlists, current, total)
      }
      coverCache.cache { current, total ->
        progress?.invoke(LibraryMediaType.Covers, current, total)
      }
      Timber.v("Library refresh was complete")
      return Outcome.Success(syncStats())
    } catch (e: IOException) {
      Timber.e(e, "Refresh couldn't complete")
      return Outcome.Failure(AppError.Message(e.message ?: "Unknown error"))
    } catch (e: SQLException) {
      Timber.e(e, "Refresh couldn't complete because the database rejected it")
      return Outcome.Failure(AppError.Message(e.message ?: "Unknown database error"))
    } finally {
      running = false
    }
  }

  override suspend fun syncStats(): LibraryStats = LibraryStats(
    genres = genreRepository.count(),
    artists = artistRepository.count(),
    albums = albumRepository.count(),
    tracks = trackRepository.count(),
    playlists = playlistRepository.count(),
    covers = albumRepository.coverCount()
  )

  private suspend fun checkIfShouldSync(auto: Boolean): Boolean = !auto || isEmpty()

  private suspend fun isEmpty(): Boolean = genreRepository.count() == 0L &&
    artistRepository.count() == 0L &&
    albumRepository.count() == 0L &&
    trackRepository.count() == 0L

  override suspend fun ensureDerived() = libraryDerivationUseCase.ensureDerived()

  override fun isRunning(): Boolean = running
}
