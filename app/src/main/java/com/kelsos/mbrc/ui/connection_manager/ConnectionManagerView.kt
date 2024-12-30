package com.kelsos.mbrc.ui.connection_manager

import com.kelsos.mbrc.mvp.BaseView

interface ConnectionManagerView : BaseView {
  fun updateModel(connectionModel: ConnectionModel)

  fun defaultChanged()

  fun dataUpdated()
}
