package com.kelsos.mbrc.features.minicontrol

sealed class MiniControlAction {
  data object PlayPrevious : MiniControlAction()

  data object PlayNext : MiniControlAction()

  data object PlayPause : MiniControlAction()
}
