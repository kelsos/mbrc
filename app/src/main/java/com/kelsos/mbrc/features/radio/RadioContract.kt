package com.kelsos.mbrc.features.radio

import com.kelsos.mbrc.common.mvp.BaseView
import com.kelsos.mbrc.common.mvp.Presenter
import com.raizlabs.android.dbflow.list.FlowCursorList

interface RadioView : BaseView {
  fun update(data: FlowCursorList<RadioStation>)

  fun error(error: Throwable)

  fun radioPlayFailed()

  fun radioPlaySuccessful()

  fun showLoading()

  fun hideLoading()
}

interface RadioPresenter : Presenter<RadioView> {
  fun load()

  fun refresh()

  fun play(path: String)
}
