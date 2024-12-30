package com.kelsos.mbrc.ui.navigation.main

import toothpick.config.Module

class MainModule : Module() {
  init {
    bind(MainViewPresenter::class.java).to(MainViewPresenterImpl::class.java).singletonInScope()
  }
}
