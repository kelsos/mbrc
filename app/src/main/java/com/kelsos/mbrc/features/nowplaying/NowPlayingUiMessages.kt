package com.kelsos.mbrc.features.nowplaying

import com.kelsos.mbrc.common.mvvm.UiMessageBase

sealed class NowPlayingUiMessages : UiMessageBase {
  class RefreshFailed(
    val throwable: Throwable,
  ) : NowPlayingUiMessages()

  object RefreshSucceeded : NowPlayingUiMessages()
}
