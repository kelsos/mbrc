package com.kelsos.mbrc.features.library

import toothpick.config.Module

class LibraryModule : Module() {
  init {
    bind(LibraryPresenter::class.java).to(LibraryPresenterImpl::class.java).singletonInScope()
    bind(LibrarySearchModel::class.java).singletonInScope()
  }
}
