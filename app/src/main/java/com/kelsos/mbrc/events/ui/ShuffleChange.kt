package com.kelsos.mbrc.events.ui

import androidx.annotation.StringDef

class ShuffleChange(
  @ShuffleState val shuffleState: String,
) {
  @StringDef(OFF, AUTODJ, SHUFFLE)
  @Retention(AnnotationRetention.SOURCE)
  annotation class ShuffleState

  companion object {
    const val OFF = "off"
    const val AUTODJ = "autodj"
    const val SHUFFLE = "shuffle"
  }
}
