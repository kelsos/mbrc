package com.kelsos.mbrc.features.settings.compose

import android.content.Context
import android.content.Intent
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.State
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import com.kelsos.mbrc.features.settings.CallAction
import com.kelsos.mbrc.features.settings.SettingsManager
import com.kelsos.mbrc.features.settings.TrackAction
import com.kelsos.mbrc.features.theme.Theme
import com.kelsos.mbrc.features.theme.ThemeManager
import com.kelsos.mbrc.logging.FileLoggingTree
import com.kelsos.mbrc.platform.RemoteService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import org.koin.compose.koinInject
import timber.log.Timber

/**
 * State holder for SettingsScreen that manages all settings-related state and business logic.
 * Uses Compose state management patterns with reactive flows from SettingsManager.
 */
@Stable
class SettingsScreenState(
  private val settingsManager: SettingsManager,
  private val themeManager: ThemeManager,
  private val context: Context
) {
  private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

  var visibleDialog: DialogType? by mutableStateOf(null)
    private set

  var licenseUrl: String by mutableStateOf("")
    private set

  var licenseTitle: String by mutableStateOf("")
    private set

  @Composable
  fun currentTheme(): State<Theme> = settingsManager.themeFlow.collectAsState(initial = Theme.Dark)

  @Composable
  fun pluginUpdatesEnabled(): State<Boolean> =
    settingsManager.pluginUpdateCheckFlow.collectAsState(initial = false)

  @Composable
  fun debugLoggingEnabled(): State<Boolean> =
    settingsManager.debugLoggingFlow.collectAsState(initial = false)

  @Composable
  fun incomingCallAction(): State<CallAction> =
    settingsManager.incomingCallActionFlow.collectAsState(initial = CallAction.None)

  @Composable
  fun trackDefaultAction(): State<TrackAction> =
    settingsManager.libraryTrackDefaultActionFlow.collectAsState(initial = TrackAction.PlayNow)

  /**
   * Updates the app theme and applies it immediately.
   */
  fun updateTheme(theme: Theme) {
    scope.launch {
      settingsManager.setTheme(theme)
    }
  }

  /**
   * Updates plugin update check preference.
   */
  fun updatePluginUpdates(enabled: Boolean) {
    scope.launch {
      settingsManager.setPluginUpdateCheck(enabled)
    }
  }

  /**
   * Updates debug logging preference and handles logging tree setup.
   */
  fun updateDebugLogging(enabled: Boolean) {
    scope.launch {
      settingsManager.setDebugLogging(enabled)
      handleDebugLogging(enabled)
    }
  }

  /**
   * Updates incoming call action preference and restarts service.
   */
  fun updateIncomingCallAction(action: CallAction) {
    scope.launch {
      settingsManager.setIncomingCallAction(action)
      // Restart the service to apply the new setting
      val intent = Intent(context, RemoteService::class.java)
      context.stopService(intent)
      context.startService(intent)
    }
  }

  /**
   * Updates track default action preference.
   */
  fun updateTrackDefaultAction(action: TrackAction) {
    scope.launch {
      settingsManager.setLibraryTrackDefaultAction(action)
    }
  }

  /**
   * Shows a dialog of the specified type.
   */
  fun showDialog(dialogType: DialogType) {
    visibleDialog = dialogType
  }

  /**
   * Hides the currently visible dialog.
   */
  fun hideDialog() {
    visibleDialog = null
    // Clear license data when closing license dialog
    if (licenseUrl.isNotEmpty()) {
      licenseUrl = ""
      licenseTitle = ""
    }
  }

  /**
   * Shows the license dialog with specific URL and title.
   */
  fun showLicenseDialog(url: String, title: String) {
    licenseUrl = url
    licenseTitle = title
    visibleDialog = DialogType.License
  }

  /**
   * Handles debug logging tree setup and removal.
   */
  private fun handleDebugLogging(enabled: Boolean) {
    if (enabled) {
      // Only plant if not already planted
      val hasFileLoggingTree = Timber.forest().any { it is FileLoggingTree }
      if (!hasFileLoggingTree) {
        Timber.plant(FileLoggingTree(context.applicationContext))
        Timber.d("Debug logging enabled")
      }
    } else {
      // Remove file logging tree
      val fileLoggingTree = Timber.forest().find { it is FileLoggingTree }
      fileLoggingTree?.let {
        Timber.uproot(it)
        Timber.d("Debug logging disabled")
      }
    }
  }

  /**
   * Cleanup coroutine scope when state is no longer needed.
   */
  fun dispose() {
    scope.cancel()
  }
}

/**
 * Types of dialogs that can be shown in the Settings Screen.
 */
sealed class DialogType {
  data object Theme : DialogType()
  data object IncomingCallAction : DialogType()
  data object TrackDefaultAction : DialogType()
  data object License : DialogType()
}

/**
 * Remember function for SettingsScreenState that handles dependency injection.
 */
@Composable
fun rememberSettingsScreenState(
  settingsManager: SettingsManager = koinInject(),
  themeManager: ThemeManager = koinInject(),
  context: Context = LocalContext.current
): SettingsScreenState = remember(settingsManager, themeManager, context) {
  SettingsScreenState(settingsManager, themeManager, context)
}
