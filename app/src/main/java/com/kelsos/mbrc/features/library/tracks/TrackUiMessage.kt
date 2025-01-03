package com.kelsos.mbrc.features.library.tracks

import com.kelsos.mbrc.common.mvvm.UiMessageBase

sealed class TrackUiMessage : UiMessageBase {
  class QueueSuccess(
    val tracksCount: Int,
  ) : TrackUiMessage()

  object QueueFailed : TrackUiMessage()
}
