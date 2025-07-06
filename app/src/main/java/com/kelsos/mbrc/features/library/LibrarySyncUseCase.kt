package com.kelsos.mbrc.features.library

import com.kelsos.mbrc.common.data.cacheIsEmpty
import com.kelsos.mbrc.features.library.albums.AlbumRepository
import com.kelsos.mbrc.features.library.artists.ArtistRepository
import com.kelsos.mbrc.features.library.genres.GenreRepository
import com.kelsos.mbrc.features.library.tracks.TrackRepository
import com.kelsos.mbrc.features.playlists.PlaylistRepository
import okio.IOException
import timber.log.Timber

typealias SyncProgress = suspend (category: LibraryMediaType, current: Int, total: Int) -> Unit

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
  suspend fun sync(auto: Boolean = false, progress: SyncProgress? = null): SyncResult

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
  private val coverCache: CoverCache
) : LibrarySyncUseCase {
  private var running: Boolean = false

  override suspend fun sync(auto: Boolean, progress: SyncProgress?): SyncResult {
    if (isRunning()) {
      Timber.v("Sync is already running")
      return SyncResult.Noop
    }

    running = true
    Timber.v("Starting library metadata sync")
    val shouldSync = checkIfShouldSync(auto)
    if (!shouldSync) {
      running = false
      return SyncResult.Noop
    }

    try {
      genreRepository.getRemote { current, total ->
        progress?.invoke(LibraryMediaType.Genres, current, total)
      }
      artistRepository.getRemote { current, total ->
        progress?.invoke(LibraryMediaType.Artists, current, total)
      }
      albumRepository.getRemote { current, total ->
        progress?.invoke(LibraryMediaType.Albums, current, total)
      }
      trackRepository.getRemote { current, total ->
        progress?.invoke(LibraryMediaType.Tracks, current, total)
      }
      playlistRepository.getRemote { current, total ->
        progress?.invoke(LibraryMediaType.Playlists, current, total)
      }
      coverCache.cache { current, total ->
        progress?.invoke(LibraryMediaType.Covers, current, total)
      }
      Timber.v("Library refresh was complete")
      return SyncResult.Success(syncStats())
    } catch (e: IOException) {
      Timber.e(e, "Refresh couldn't complete")
      return SyncResult.Failed(e.message ?: "Unknown error")
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

  private suspend fun isEmpty(): Boolean = genreRepository.cacheIsEmpty() &&
    artistRepository.cacheIsEmpty() &&
    albumRepository.cacheIsEmpty() &&
    trackRepository.cacheIsEmpty()

  override fun isRunning(): Boolean = running
}
