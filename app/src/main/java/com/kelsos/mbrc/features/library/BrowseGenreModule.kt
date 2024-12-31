package com.kelsos.mbrc.features.library

import toothpick.config.Module

class BrowseGenreModule : Module() {
  init {
    bind(BrowseGenrePresenter::class.java)
      .to(BrowseGenrePresenterImpl::class.java)
      .singletonInScope()
  }
}
