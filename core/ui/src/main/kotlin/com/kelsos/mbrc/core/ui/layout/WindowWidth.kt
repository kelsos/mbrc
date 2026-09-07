package com.kelsos.mbrc.core.ui.layout

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalWindowInfo
import com.kelsos.mbrc.core.common.layout.WindowWidthClass

/**
 * The width class of the window the content is being laid out in.
 *
 * Provided once at the top of the app so screens agree on a single answer, and so previews and
 * screenshot tests can pin a class without having to fake a window.
 */
val LocalWindowWidthClass = staticCompositionLocalOf { WindowWidthClass.Compact }

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
