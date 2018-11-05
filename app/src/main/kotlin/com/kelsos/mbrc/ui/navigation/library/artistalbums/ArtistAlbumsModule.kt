package com.kelsos.mbrc.ui.navigation.library.artistalbums

import toothpick.config.Module

class ArtistAlbumsModule : Module() {
  init {
    bind(ArtistAlbumsPresenter::class.java).to(
        ArtistAlbumsPresenterImpl::class.java).singletonInScope()
  }
}