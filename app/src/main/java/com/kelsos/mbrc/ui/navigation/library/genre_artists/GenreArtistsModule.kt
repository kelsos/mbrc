package com.kelsos.mbrc.ui.navigation.library.genre_artists

import toothpick.config.Module

class GenreArtistsModule : Module() {
  init {
    bind(GenreArtistsPresenter::class.java)
      .to(GenreArtistsPresenterImpl::class.java)
      .singletonInScope()
  }
}
