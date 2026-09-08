package com.kelsos.mbrc.core.ui.compose

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

private const val MIN_HEIGHT_FRACTION = 0.1f
private const val MAX_HEIGHT_FRACTION = 1f
private val BAR_CORNER_RADIUS = 1.dp

/**
 * Animated audio bars indicator similar to Spotify's now playing indicator.
 * Shows 3 vertical bars that animate up and down at different speeds.
 *
 * @param modifier Modifier for the component
 * @param color Color of the bars
 * @param barWidth Width of each bar
 * @param barMaxHeight Maximum height of the bars
 * @param barSpacing Spacing between bars
 */
@Composable
fun AudioBarsIndicator(
  modifier: Modifier = Modifier,
  color: Color = MaterialTheme.colorScheme.primary,
  barWidth: Dp = 3.dp,
  barMaxHeight: Dp = 16.dp,
  barSpacing: Dp = 2.dp
) {
  val infiniteTransition = rememberInfiniteTransition(label = "audio_bars")

  // Each bar animates at a different speed and phase for natural look
  val bar1Height = infiniteTransition.animateFloat(
    initialValue = 0.3f,
    targetValue = 1f,
    animationSpec = infiniteRepeatable(
      animation = tween(durationMillis = 400, easing = LinearEasing),
      repeatMode = RepeatMode.Reverse
    ),
    label = "bar1"
  )

  val bar2Height = infiniteTransition.animateFloat(
    initialValue = 0.5f,
    targetValue = 0.2f,
    animationSpec = infiniteRepeatable(
      animation = tween(durationMillis = 300, easing = LinearEasing),
      repeatMode = RepeatMode.Reverse
    ),
    label = "bar2"
  )

  val bar3Height = infiniteTransition.animateFloat(
    initialValue = 0.2f,
    targetValue = 0.8f,
    animationSpec = infiniteRepeatable(
      animation = tween(durationMillis = 500, easing = LinearEasing),
      repeatMode = RepeatMode.Reverse
    ),
    label = "bar3"
  )

  val bars = remember(bar1Height, bar2Height, bar3Height) {
    arrayOf(bar1Height, bar2Height, bar3Height)
  }

  // The heights are read here, inside the draw lambda, rather than through `by` at composition.
  // Read at composition they fed a height modifier, so each of the sixty frames a second this
  // animates recomposed and re-laid-out the bars, and with them the queue row and the list
  // holding it. Drawn instead, an animation frame reaches the draw phase and stops there.
  Canvas(
    modifier = modifier.size(
      width = barWidth * bars.size + barSpacing * (bars.size - 1),
      height = barMaxHeight
    )
  ) {
    val widthPx = barWidth.toPx()
    val stridePx = widthPx + barSpacing.toPx()
    val radius = CornerRadius(BAR_CORNER_RADIUS.toPx(), BAR_CORNER_RADIUS.toPx())

    for (index in bars.indices) {
      val fraction = bars[index].value.coerceIn(MIN_HEIGHT_FRACTION, MAX_HEIGHT_FRACTION)
      val barHeight = size.height * fraction
      drawRoundRect(
        color = color,
        topLeft = Offset(index * stridePx, size.height - barHeight),
        size = Size(widthPx, barHeight),
        cornerRadius = radius
      )
    }
  }
}
