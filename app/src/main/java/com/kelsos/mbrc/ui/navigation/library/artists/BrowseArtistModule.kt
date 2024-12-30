package com.kelsos.mbrc.ui.navigation.library.artists

import toothpick.config.Module

class BrowseArtistModule : Module() {
  init {
    bind(BrowseArtistPresenter::class.java)
      .to(BrowseArtistPresenterImpl::class.java)
      .singletonInScope()
  }
}
