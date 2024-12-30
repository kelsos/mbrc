package com.kelsos.mbrc.ui.connection_manager


import toothpick.config.Module

class ConnectionManagerModule private constructor() : Module() {
  init {
    bind<ConnectionManagerPresenter>(ConnectionManagerPresenter::class.java).to(
      ConnectionManagerPresenterImpl::class.java
    ).singletonInScope()
  }

  companion object {

    fun create(): ConnectionManagerModule {
      return ConnectionManagerModule()
    }
  }
}
