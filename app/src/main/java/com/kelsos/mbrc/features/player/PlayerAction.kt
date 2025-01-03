package com.kelsos.mbrc.features.player

sealed class PlayerAction {
  object Stop : PlayerAction()

  object ToggleShuffle : PlayerAction()

  object ToggleRepeat : PlayerAction()

  class Seek(
    val position: Int,
  ) : PlayerAction()

  object ToggleScrobbling : PlayerAction()

  object ResumePlayOrPause : PlayerAction()

  object PlayPrevious : PlayerAction()

  object PlayNext : PlayerAction()

  object ToggleFavorite : PlayerAction()

  object ToggleMute : PlayerAction()

  class ChangeVolume(
    val volume: Int,
  ) : PlayerAction()
}
