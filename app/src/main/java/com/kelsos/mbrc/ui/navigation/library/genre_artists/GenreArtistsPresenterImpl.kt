package com.kelsos.mbrc.ui.navigation.library.genre_artists

import com.kelsos.mbrc.annotations.Queue
import com.kelsos.mbrc.data.library.Artist
import com.kelsos.mbrc.helper.QueueHandler
import com.kelsos.mbrc.mvp.BasePresenter
import com.kelsos.mbrc.repository.ArtistRepository
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

class GenreArtistsPresenterImpl
  @Inject
  constructor(
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
