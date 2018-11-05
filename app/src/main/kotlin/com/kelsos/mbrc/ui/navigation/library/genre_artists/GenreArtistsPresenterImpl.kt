package com.kelsos.mbrc.ui.navigation.library.genre_artists

import com.kelsos.mbrc.mvp.BasePresenter
import com.kelsos.mbrc.repository.ArtistRepository
import timber.log.Timber
import javax.inject.Inject

class GenreArtistsPresenterImpl
@Inject constructor(private var repository: ArtistRepository) :
    BasePresenter<GenreArtistsView>(),
    GenreArtistsPresenter {
  override fun load(genre: String) {
    addDisposable(repository.getArtistByGenre(genre).subscribe ({
      view?.update(it)
    }) {
      Timber.v(it)
    })
  }

}
