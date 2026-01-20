package com.kelsos.mbrc.core.common.state

sealed class LfmRating {
  data object Loved : LfmRating()

  data object Banned : LfmRating()

  data object Normal : LfmRating()

  fun toActionString(): String = when (this) {
    Loved -> LOVE
    Banned -> BAN
    Normal -> NORMAL
  }

  companion object {
    private const val LOVE = "Love"
    private const val BAN = "Ban"
    private const val NORMAL = "Normal"

    fun fromString(value: String?): LfmRating = when (value) {
      LOVE -> Loved
      BAN -> Banned
      else -> Normal
    }
  }
}
