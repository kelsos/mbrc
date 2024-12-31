package com.kelsos.mbrc.features.settings

import com.kelsos.mbrc.common.mvp.BaseView

interface ConnectionManagerView : BaseView {
  fun updateModel(connectionModel: ConnectionModel)

  fun defaultChanged()

  fun dataUpdated()
}
