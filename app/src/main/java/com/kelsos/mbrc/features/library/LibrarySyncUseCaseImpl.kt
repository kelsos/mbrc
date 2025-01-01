package com.kelsos.mbrc.features.library

import com.kelsos.mbrc.common.utilities.AppCoroutineDispatchers
import com.kelsos.mbrc.events.bus.RxBus
import com.kelsos.mbrc.events.ui.LibraryRefreshCompleteEvent
import com.kelsos.mbrc.features.playlists.PlaylistRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import timber.log.Timber

class LibrarySyncUseCaseImpl(
  private val genreRepository: GenreRepository,
  private val artistRepository: ArtistRepository,
  private val albumRepository: AlbumRepository,
  private val trackRepository: TrackRepository,
  private val playlistRepository: PlaylistRepository,
  private val coverCache: CoverCache,
  private val bus: RxBus,
  dispatchers: AppCoroutineDispatchers,
) : LibrarySyncUseCase {
  private var running: Boolean = false
  private var onCompleteListener: LibrarySyncUseCase.OnCompleteListener? = null
  private var onStartListener: LibrarySyncUseCase.OnStartListener? = null

  private val job = SupervisorJob()
  private val scope = CoroutineScope(job + dispatchers.io)

  override fun sync(auto: Boolean) {
    if (isRunning()) {
      Timber.v("Sync is already running")
      return
    }

    running = true
    scope.launch {
      Timber.v("Starting library metadata sync")

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
        bus.post(LibraryRefreshCompleteEvent())
        onCompleteListener?.onSuccess(
          LibraryStats(
            genres = genreRepository.count(),
            artists = artistRepository.count(),
            albums = albumRepository.count(),
            tracks = trackRepository.count(),
            playlists = playlistRepository.count(),
          ),
        )
        Timber.v("Library refresh was complete")
      } catch (e: Exception) {
        Timber.e(e, "Refresh couldn't complete")
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

  private suspend fun checkIfShouldSync(auto: Boolean): Boolean = if (auto) isEmpty() else true

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
