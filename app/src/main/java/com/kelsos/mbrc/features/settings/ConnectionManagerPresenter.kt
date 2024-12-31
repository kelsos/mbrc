package com.kelsos.mbrc.features.settings

import com.kelsos.mbrc.common.mvp.Presenter

interface ConnectionManagerPresenter : Presenter<ConnectionManagerView> {
  fun load()

  fun setDefault(settings: ConnectionSettings)

  fun save(settings: ConnectionSettings)

  fun delete(settings: ConnectionSettings)
}
