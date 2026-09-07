package com.kelsos.mbrc.core.ui.layout

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalWindowInfo
import com.kelsos.mbrc.core.common.layout.WindowWidthClass

/**
 * An override for the width class, provided once at the top of the app so every screen agrees on
 * one answer even when a screen is laid out into less than the full window.
 *
 * Null means "ask the window", which is what a preview or a screenshot test gets for free: the
 * preview's own size then decides the layout, so a tablet-sized preview renders the tablet layout
 * without having to fake a provider.
 */
val LocalWindowWidthClass = staticCompositionLocalOf<WindowWidthClass?> { null }

/**
 * The width class in effect: the provided override if there is one, otherwise measured from the
 * window. Prefer this over reading [LocalWindowWidthClass] directly.
 */
@Composable
@ReadOnlyComposable
fun windowWidthClass(): WindowWidthClass =
  LocalWindowWidthClass.current ?: currentWindowWidthClass()

/**
 * Derives the width class from the current window rather than the device configuration, so a
 * multi-window or freeform split reports the space actually available.
 */
@Composable
@ReadOnlyComposable
fun currentWindowWidthClass(): WindowWidthClass {
  val widthPx = LocalWindowInfo.current.containerSize.width
  val widthDp = with(LocalDensity.current) { widthPx.toDp() }
  return WindowWidthClass.fromWidthDp(widthDp.value.toInt())
}
