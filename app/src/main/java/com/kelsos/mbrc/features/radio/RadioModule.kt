package com.kelsos.mbrc.features.radio

import toothpick.config.Module

class RadioModule : Module() {
  init {
    bind(RadioPresenter::class.java)
      .to(RadioPresenterImpl::class.java)
      .singletonInScope()
  }
}
