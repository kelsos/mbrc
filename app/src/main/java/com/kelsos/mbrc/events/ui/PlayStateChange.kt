package com.kelsos.mbrc.events.ui

import com.kelsos.mbrc.annotations.PlayerState.State

data class PlayStateChange(
  @State val state: String,
  val position: Long,
)
