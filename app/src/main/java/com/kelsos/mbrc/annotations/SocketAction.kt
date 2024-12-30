package com.kelsos.mbrc.annotations

import androidx.annotation.IntDef

object SocketAction {
  const val RESET = 1
  const val START = 2
  const val RETRY = 3
  const val TERMINATE = 4
  const val STOP = 5

  @Retention(AnnotationRetention.SOURCE)
  @IntDef(RESET, START, RETRY, TERMINATE, STOP)
  annotation class Action

  fun name(
    @Action action: Int,
  ): String =
    when (action) {
      RESET -> "Reset"
      START -> "Start"
      RETRY -> "Retry"
      TERMINATE -> "Terminate"
      STOP -> "Stop"
      else -> throw IllegalArgumentException("action $action is not recognised")
    }
}
