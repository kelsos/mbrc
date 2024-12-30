package com.kelsos.mbrc.ui.connection_manager

import com.kelsos.mbrc.data.ConnectionSettings
import com.kelsos.mbrc.mvp.Presenter

interface ConnectionManagerPresenter : Presenter<ConnectionManagerView> {
  fun load()

  fun setDefault(settings: ConnectionSettings)

  fun save(settings: ConnectionSettings)

  fun delete(settings: ConnectionSettings)
}
