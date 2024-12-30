package com.kelsos.mbrc.ui.navigation.lyrics

import toothpick.config.Module

class LyricsModule : Module() {
  init {
    bind(LyricsPresenter::class.java).to(LyricsPresenterImpl::class.java).singletonInScope()
  }

}
