package com.kelsos.mbrc.core.common.layout

/**
 * Breakpoints, in dp, matching the Material 3 window size class definitions. Kept here rather than
 * in `core:ui` so that non-Compose code (settings enums, tests) can share the same numbers.
 */
object WindowBreakpoints {
  const val MEDIUM_MIN_WIDTH_DP = 600
  const val EXPANDED_MIN_WIDTH_DP = 840
}

/**
 * The horizontal space a screen has to lay itself out in.
 *
 * This is deliberately derived from available width rather than device orientation: a tablet held
 * in portrait has more room than a phone held in landscape, and branching on orientation gets that
 * backwards.
 */
enum class WindowWidthClass {
  /** Phones in portrait, and anything sharing the screen with another app. */
  Compact,

  /** Large phones in landscape, small tablets, unfolded foldables. */
  Medium,

  /** Tablets in either orientation, desktop-sized windows. */
  Expanded;

  /** True when there is room to show two panes side by side. */
  val supportsTwoPanes: Boolean
    get() = this == Expanded

  /** True when navigation should be persistently visible rather than behind a modal drawer. */
  val prefersPersistentNavigation: Boolean
    get() = this != Compact

  companion object {
    fun fromWidthDp(widthDp: Int): WindowWidthClass = when {
      widthDp >= WindowBreakpoints.EXPANDED_MIN_WIDTH_DP -> Expanded
      widthDp >= WindowBreakpoints.MEDIUM_MIN_WIDTH_DP -> Medium
      else -> Compact
    }
  }
}
