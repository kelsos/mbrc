package com.kelsos.mbrc.ui.minicontrol

import toothpick.config.Module

class MiniControlModule : Module() {
  init {
    bind(MiniControlPresenter::class.java)
        .to(MiniControlPresenterImpl::class.java)
        .singletonInScope()
  }
}
