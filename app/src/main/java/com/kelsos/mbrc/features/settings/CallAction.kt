package com.kelsos.mbrc.features.settings

sealed class CallAction(
  val string: String,
) {
  object None : CallAction(NONE)

  object Pause : CallAction(PAUSE)

  object Stop : CallAction(STOP)

  object Reduce : CallAction(REDUCE)

  companion object {
    const val NONE = "none"
    const val PAUSE = "pause"
    const val STOP = "stop"
    const val REDUCE = "reduce"

    fun fromString(action: String): CallAction =
      when (action.lowercase()) {
        PAUSE -> Pause
        STOP -> Stop
        REDUCE -> Reduce
        else -> None
      }
  }
}
