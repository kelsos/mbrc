package com.kelsos.mbrc.features.playlists

import toothpick.config.Module

class PlaylistModule : Module() {
  init {
    bind(PlaylistPresenter::class.java).to(PlaylistPresenterImpl::class.java).singletonInScope()
  }
}
