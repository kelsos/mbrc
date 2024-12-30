package com.kelsos.mbrc.ui.navigation.radio

import toothpick.config.Module

class RadioModule : Module() {
  init {
    bind(RadioPresenter::class.java)
        .to(RadioPresenterImpl::class.java)
        .singletonInScope()
  }
}
