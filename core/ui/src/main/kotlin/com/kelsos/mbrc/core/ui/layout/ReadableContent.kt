package com.kelsos.mbrc.core.ui.layout

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.widthIn
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

object ReadableContentDefaults {
  /**
   * Beyond roughly this width a settings row or a paragraph stops being easier to read and starts
   * being a line the eye has to track back across, with the control it belongs to a screen away
   * from its label.
   */
  val MaxWidth: Dp = 720.dp
}

/**
 * Centres [content] and stops it growing past [maxWidth].
 *
 * Lists of covers want the whole window; forms, settings rows and prose do not.
 */
@Composable
fun ReadableContent(
  modifier: Modifier = Modifier,
  maxWidth: Dp = ReadableContentDefaults.MaxWidth,
  content: @Composable () -> Unit
) {
  Box(
    modifier = modifier.fillMaxSize(),
    contentAlignment = Alignment.TopCenter
  ) {
    Box(modifier = Modifier.widthIn(max = maxWidth)) {
      content()
    }
  }
}
