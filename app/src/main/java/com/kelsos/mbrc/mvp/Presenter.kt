package com.kelsos.mbrc.mvp

interface Presenter<in T : BaseView> {
  fun attach(view: T)

  fun detach()

  val isAttached: Boolean
}
