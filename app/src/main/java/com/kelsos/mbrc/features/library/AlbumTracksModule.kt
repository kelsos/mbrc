package com.kelsos.mbrc.features.library

import toothpick.config.Module

class AlbumTracksModule : Module() {
  init {
    bind(AlbumTracksPresenter::class.java)
      .to(AlbumTracksPresenterImpl::class.java)
      .singletonInScope()
  }
}
