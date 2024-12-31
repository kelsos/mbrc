package com.kelsos.mbrc.features.library

import toothpick.config.Module

class ArtistAlbumsModule : Module() {
  init {
    bind(ArtistAlbumsPresenter::class.java)
      .to(
        ArtistAlbumsPresenterImpl::class.java,
      ).singletonInScope()
  }
}
