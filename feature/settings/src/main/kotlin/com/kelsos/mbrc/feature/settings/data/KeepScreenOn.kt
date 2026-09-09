package com.kelsos.mbrc.feature.settings.data

/**
 * When the app holds the display awake.
 *
 * [WhileCharging] is the tier worth having: a remote propped on a counter is almost always on a
 * cable, and that is the case where the display timeout is pure nuisance rather than a battery
 * saving. It mirrors the platform's own "stay awake while charging" developer option.
 */
sealed class KeepScreenOn(val string: String) {
  object Never : KeepScreenOn(NEVER)

  object WhileCharging : KeepScreenOn(WHILE_CHARGING)

  object Always : KeepScreenOn(ALWAYS)

  companion object {
    const val NEVER = "never"
    const val WHILE_CHARGING = "while_charging"
    const val ALWAYS = "always"

    fun fromString(value: String): KeepScreenOn = when (value.lowercase()) {
      WHILE_CHARGING -> WhileCharging
      ALWAYS -> Always
      else -> Never
    }
  }
}
