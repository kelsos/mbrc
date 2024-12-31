package com.kelsos.mbrc.features.library

import toothpick.config.Module

class GenreArtistsModule : Module() {
  init {
    bind(GenreArtistsPresenter::class.java)
      .to(GenreArtistsPresenterImpl::class.java)
      .singletonInScope()
  }
}
