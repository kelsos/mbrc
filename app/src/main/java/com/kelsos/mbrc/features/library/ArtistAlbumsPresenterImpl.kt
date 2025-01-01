package com.kelsos.mbrc.features.library

import com.kelsos.mbrc.common.mvp.BasePresenter
import com.kelsos.mbrc.features.queue.QueueHandler
import kotlinx.coroutines.launch
import timber.log.Timber

class ArtistAlbumsPresenterImpl(
  private val repository: AlbumRepository,
  private val queue: QueueHandler,
) : BasePresenter<ArtistAlbumsView>(),
  ArtistAlbumsPresenter {
  override fun load(artist: String) {
    scope.launch {
      try {
        view?.update(repository.getAlbumsByArtist(artist))
      } catch (e: Exception) {
        Timber.v(e)
      }
    }
  }

  override fun queue(
    action: String,
    album: Album,
  ) {
    scope.launch {
      val artist = album.artist ?: throw IllegalArgumentException("artist is null")
      val albumName = album.album ?: throw java.lang.IllegalArgumentException("album is null")
      val (success, tracks) = queue.queueAlbum(action, albumName, artist)
      view?.queue(success, tracks)
    }
  }
}
