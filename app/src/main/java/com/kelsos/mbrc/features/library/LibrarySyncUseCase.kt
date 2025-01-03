package com.kelsos.mbrc.features.library

import com.kelsos.mbrc.common.data.cacheIsEmpty
import com.kelsos.mbrc.common.utilities.AppCoroutineDispatchers
import com.kelsos.mbrc.features.library.albums.AlbumRepository
import com.kelsos.mbrc.features.library.artists.ArtistRepository
import com.kelsos.mbrc.features.library.genres.GenreRepository
import com.kelsos.mbrc.features.library.tracks.TrackRepository
import com.kelsos.mbrc.features.playlists.PlaylistRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import okio.IOException
import timber.log.Timber

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
  fun sync(auto: Boolean = false)

  /**
   * Provides access to the interactor's current status.
   *
   * @return Should return true if the interactor is active and running, or false if not
   */
  fun isRunning(): Boolean

  fun setOnCompleteListener(onCompleteListener: OnCompleteListener?)

  fun setOnStartListener(onStartListener: OnStartListener?)

  suspend fun syncStats(): LibraryStats

  interface OnCompleteListener {
    fun onTermination()

    fun onFailure(throwable: Throwable)

    fun onSuccess(stats: LibraryStats)
  }

  fun interface OnStartListener {
    fun onStart()
  }
}

class LibrarySyncUseCaseImpl(
  private val genreRepository: GenreRepository,
  private val artistRepository: ArtistRepository,
  private val albumRepository: AlbumRepository,
  private val trackRepository: TrackRepository,
  private val playlistRepository: PlaylistRepository,
  private val coverCache: CoverCache,
  dispatchers: AppCoroutineDispatchers,
) : LibrarySyncUseCase {
  private var running: Boolean = false
  private var onCompleteListener: LibrarySyncUseCase.OnCompleteListener? = null
  private var onStartListener: LibrarySyncUseCase.OnStartListener? = null

  private val job = SupervisorJob()
  private val scope = CoroutineScope(job + dispatchers.io)

  override fun sync(auto: Boolean) {
    if (isRunning()) {
      Timber.Forest.v("Sync is already running")
      return
    }

    running = true
    scope.launch {
      Timber.Forest.v("Starting library metadata sync")

      onStartListener?.onStart()

      val shouldSync = checkIfShouldSync(auto)
      if (!shouldSync) {
        onCompleteListener?.onTermination()
        running = false
        return@launch
      }

      try {
        genreRepository.getRemote()
        artistRepository.getRemote()
        albumRepository.getRemote()
        trackRepository.getRemote()
        playlistRepository.getRemote()
        coverCache.cache()
        onCompleteListener?.onSuccess(
          LibraryStats(
            genres = genreRepository.count(),
            artists = artistRepository.count(),
            albums = albumRepository.count(),
            tracks = trackRepository.count(),
            playlists = playlistRepository.count(),
          ),
        )
        Timber.Forest.v("Library refresh was complete")
      } catch (e: IOException) {
        Timber.Forest.e(e, "Refresh couldn't complete")
        onCompleteListener?.onFailure(e)
      } finally {
        running = false
        onCompleteListener?.onTermination()
      }
    }
  }

  override suspend fun syncStats(): LibraryStats =
    LibraryStats(
      genres = genreRepository.count(),
      artists = artistRepository.count(),
      albums = albumRepository.count(),
      tracks = trackRepository.count(),
      playlists = playlistRepository.count(),
    )

  private suspend fun checkIfShouldSync(auto: Boolean): Boolean = !auto || isEmpty()

  private suspend fun isEmpty(): Boolean =
    genreRepository.cacheIsEmpty() &&
      artistRepository.cacheIsEmpty() &&
      albumRepository.cacheIsEmpty() &&
      trackRepository.cacheIsEmpty()

  override fun setOnCompleteListener(onCompleteListener: LibrarySyncUseCase.OnCompleteListener?) {
    this.onCompleteListener = onCompleteListener
  }

  override fun setOnStartListener(onStartListener: LibrarySyncUseCase.OnStartListener?) {
    this.onStartListener = onStartListener
  }

  override fun isRunning(): Boolean = running
}
