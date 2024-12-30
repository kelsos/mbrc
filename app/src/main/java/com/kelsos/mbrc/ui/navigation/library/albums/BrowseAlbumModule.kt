package com.kelsos.mbrc.ui.navigation.library.albums

import toothpick.config.Module

class BrowseAlbumModule : Module() {
  init {
    bind(BrowseAlbumPresenter::class.java).to(BrowseAlbumPresenterImpl::class.java)
      .singletonInScope()
  }
}
