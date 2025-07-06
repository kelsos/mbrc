package com.kelsos.mbrc.common.state

sealed class Repeat(val mode: String) {
  object All : Repeat(ALL)

  object None : Repeat(NONE)

  object One : Repeat(ONE)

  companion object {
    const val ALL = "all"
    const val NONE = "none"
    const val ONE = "one"

    fun fromString(mode: String?): Repeat = when (mode?.lowercase()) {
      ALL -> All
      ONE -> One
      else -> None
    }
  }
}
