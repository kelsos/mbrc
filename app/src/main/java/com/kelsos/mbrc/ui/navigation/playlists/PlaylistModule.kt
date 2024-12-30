package com.kelsos.mbrc.ui.navigation.playlists

import toothpick.config.Module

class PlaylistModule : Module() {
  init {
    bind(PlaylistPresenter::class.java).to(PlaylistPresenterImpl::class.java).singletonInScope()
  }
}
