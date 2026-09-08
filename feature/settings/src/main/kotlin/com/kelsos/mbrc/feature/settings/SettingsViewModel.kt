package com.kelsos.mbrc.feature.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kelsos.mbrc.core.common.settings.TrackAction
import com.kelsos.mbrc.core.platform.service.ServiceRestarter
import com.kelsos.mbrc.feature.settings.data.CallAction
import com.kelsos.mbrc.feature.settings.domain.SettingsManager
import com.kelsos.mbrc.feature.settings.theme.Theme
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * Types of dialogs that can be shown in the Settings Screen.
 */
sealed class SettingsDialogType {
  data object Theme : SettingsDialogType()
  data object IncomingCallAction : SettingsDialogType()
  data object TrackDefaultAction : SettingsDialogType()
}

/**
 * Interface for managing debug logging tree setup.
 */
interface DebugLoggingManager {
  fun setDebugLogging(enabled: Boolean)
}

/**
 * ViewModel for the Settings screen.
 * Manages all settings-related state and business logic using proper ViewModel patterns.
 */
class SettingsViewModel(
  private val settingsManager: SettingsManager,
  private val serviceRestarter: ServiceRestarter,
  private val debugLoggingManager: DebugLoggingManager
) : ViewModel() {

  // State flows from settings manager
  val currentTheme: StateFlow<Theme> = settingsManager.themeFlow
    .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), Theme.System)

  val pluginUpdatesEnabled: StateFlow<Boolean> = settingsManager.pluginUpdateCheckFlow
    .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

  val debugLoggingEnabled: StateFlow<Boolean> = settingsManager.debugLoggingFlow
    .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

  val incomingCallAction: StateFlow<CallAction> = settingsManager.incomingCallActionFlow
    .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), CallAction.None)

  val trackDefaultAction: StateFlow<TrackAction> = settingsManager.libraryTrackDefaultActionFlow
    .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), TrackAction.PlayNow)

  val halfStarRatingEnabled: StateFlow<Boolean> = settingsManager.halfStarRatingFlow
    .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

  val showRatingOnPlayerEnabled: StateFlow<Boolean> = settingsManager.showRatingOnPlayerFlow
    .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

  val keepScreenOnEnabled: StateFlow<Boolean> = settingsManager.keepScreenOnFlow
    .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

  // Dialog state
  private val _visibleDialog = MutableStateFlow<SettingsDialogType?>(null)
  val visibleDialog: StateFlow<SettingsDialogType?> = _visibleDialog.asStateFlow()

  /**
   * Updates the app theme.
   */
  fun updateTheme(theme: Theme) {
    viewModelScope.launch {
      settingsManager.setTheme(theme)
    }
  }

  /**
   * Updates plugin update check preference.
   */
  fun updatePluginUpdates(enabled: Boolean) {
    viewModelScope.launch {
      settingsManager.setPluginUpdateCheck(enabled)
    }
  }

  /**
   * Updates debug logging preference and handles logging tree setup.
   */
  fun updateDebugLogging(enabled: Boolean) {
    viewModelScope.launch {
      settingsManager.setDebugLogging(enabled)
      debugLoggingManager.setDebugLogging(enabled)
    }
  }

  /**
   * Updates incoming call action preference and restarts service.
   */
  fun updateIncomingCallAction(action: CallAction) {
    viewModelScope.launch {
      settingsManager.setIncomingCallAction(action)
      serviceRestarter.restartService()
    }
  }

  /**
   * Updates track default action preference.
   */
  fun updateTrackDefaultAction(action: TrackAction) {
    viewModelScope.launch {
      settingsManager.setLibraryTrackDefaultAction(action)
    }
  }

  /**
   * Updates half-star rating preference.
   */
  fun updateHalfStarRating(enabled: Boolean) {
    viewModelScope.launch {
      settingsManager.setHalfStarRating(enabled)
    }
  }

  /**
   * Updates show rating on player preference.
   */
  fun updateShowRatingOnPlayer(enabled: Boolean) {
    viewModelScope.launch {
      settingsManager.setShowRatingOnPlayer(enabled)
    }
  }

  /**
   * Updates whether the screen stays awake while the app is visible.
   */
  fun updateKeepScreenOn(enabled: Boolean) {
    viewModelScope.launch {
      settingsManager.setKeepScreenOn(enabled)
    }
  }

  /**
   * Shows a dialog of the specified type.
   */
  fun showDialog(dialogType: SettingsDialogType) {
    _visibleDialog.value = dialogType
  }

  /**
   * Hides the currently visible dialog.
   */
  fun hideDialog() {
    _visibleDialog.value = null
  }
}
