package com.kelsos.mbrc.features.theme

sealed class Theme(val value: String) {
  data object System : Theme(SYSTEM)
  data object Light : Theme(LIGHT)
  data object Dark : Theme(DARK)

  companion object {
    const val SYSTEM = "system"
    const val LIGHT = "light"
    const val DARK = "dark"

    fun fromString(value: String): Theme = when (value) {
      SYSTEM -> System
      LIGHT -> Light
      DARK -> Dark
      else -> System // Default to system if unknown value
    }
  }
}
