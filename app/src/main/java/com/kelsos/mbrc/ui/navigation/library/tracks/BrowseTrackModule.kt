package com.kelsos.mbrc.ui.navigation.library.tracks

import toothpick.config.Module

class BrowseTrackModule : Module() {
  init {
    bind(BrowseTrackPresenter::class.java)
      .to(BrowseTrackPresenterImpl::class.java)
      .singletonInScope()
  }
}
