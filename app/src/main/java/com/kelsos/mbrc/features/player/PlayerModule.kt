package com.kelsos.mbrc.features.player

import toothpick.config.Module

class PlayerModule : Module() {
  init {
    bind(PlayerViewPresenter::class.java).to(PlayerViewPresenterImpl::class.java).singletonInScope()
  }
}
