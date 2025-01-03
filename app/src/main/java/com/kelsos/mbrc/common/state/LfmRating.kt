package com.kelsos.mbrc.common.state

sealed class LfmRating {
  data object Loved : LfmRating()

  data object Banned : LfmRating()

  data object Normal : LfmRating()

  companion object {
    private const val LOVE = "Love"
    private const val BAN = "Ban"

    fun fromString(value: String?): LfmRating =
      when (value) {
        LOVE -> Loved
        BAN -> Banned
        else -> Normal
      }
  }
}
