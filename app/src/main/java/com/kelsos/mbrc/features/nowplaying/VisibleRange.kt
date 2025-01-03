package com.kelsos.mbrc.features.nowplaying

data class VisibleRange(
  val firstItem: Int,
  val lastItem: Int,
) {
  val itemCount: Int
    get() = lastItem - firstItem
}

fun interface VisibleRangeGetter {
  fun visibleRange(): VisibleRange
}
