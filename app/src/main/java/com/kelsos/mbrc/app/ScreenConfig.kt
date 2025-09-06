package com.kelsos.mbrc.app

import androidx.compose.runtime.Composable
import kotlinx.coroutines.flow.Flow

/**
 * Configuration for screen-level UI elements that should be rendered
 * by the main app scaffold.
 */
data class ScreenConfig(
  val floatingActionButton: (@Composable () -> Unit)? = null,
  val snackbarMessages: Flow<String>? = null,
  val topBarActions: (@Composable () -> Unit)? = null,
  val hasCustomTopBar: Boolean = false
) {
  companion object {
    /**
     * Default empty configuration for screens that don't need
     * any special scaffold elements.
     */
    val Empty = ScreenConfig()
  }
}

/**
 * Interface for screens that need to configure the main app scaffold.
 * This allows screens to declare what they need (FAB, snackbar messages, etc.)
 * without managing their own scaffold.
 */
interface ConfigurableScreen {
  @Composable
  fun getScreenConfig(): ScreenConfig
}
