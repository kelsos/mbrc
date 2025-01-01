package com.kelsos.mbrc.features.library

import com.kelsos.mbrc.annotations.Queue
import com.kelsos.mbrc.common.mvp.BasePresenter
import com.kelsos.mbrc.features.queue.QueueHandler
import kotlinx.coroutines.launch
import timber.log.Timber

class GenreArtistsPresenterImpl(
  private val repository: ArtistRepository,
  private val queue: QueueHandler,
) : BasePresenter<GenreArtistsView>(),
  GenreArtistsPresenter {
  override fun load(genre: String) {
    scope.launch {
      try {
        view?.update(repository.getArtistByGenre(genre))
      } catch (e: Exception) {
        Timber.v(e)
      }
    }
  }

  override fun queue(
    @Queue.Action action: String,
    entry: Artist,
  ) {
    scope.launch {
      val artist = entry.artist ?: throw IllegalArgumentException("artist is null")
      val (success, tracks) = queue.queueArtist(action, artist)
      view?.queue(success, tracks)
    }
  }
}
