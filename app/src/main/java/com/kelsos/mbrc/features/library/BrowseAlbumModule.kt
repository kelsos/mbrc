package com.kelsos.mbrc.features.library

import toothpick.config.Module

class BrowseAlbumModule : Module() {
  init {
    bind(BrowseAlbumPresenter::class.java)
      .to(BrowseAlbumPresenterImpl::class.java)
      .singletonInScope()
  }
}
