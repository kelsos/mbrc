package com.kelsos.mbrc.features.player

import com.kelsos.mbrc.common.mvvm.UiMessageBase

sealed class PlayerUiMessage : UiMessageBase {
  object ShowChangelog : PlayerUiMessage()
}
