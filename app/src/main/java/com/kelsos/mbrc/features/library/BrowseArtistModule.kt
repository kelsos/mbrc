package com.kelsos.mbrc.features.library

import toothpick.config.Module

class BrowseArtistModule : Module() {
  init {
    bind(BrowseArtistPresenter::class.java)
      .to(BrowseArtistPresenterImpl::class.java)
      .singletonInScope()
  }
}
