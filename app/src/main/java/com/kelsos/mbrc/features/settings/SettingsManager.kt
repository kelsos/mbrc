package com.kelsos.mbrc.features.settings

import com.kelsos.mbrc.features.theme.Theme
import java.time.Instant
import kotlinx.coroutines.flow.Flow

/**
 * Modern reactive and async settings manager interface.
 * All settings are exposed as reactive flows and updates are async.
 */
interface SettingsManager {
  // Reactive flows for observing all settings (type-safe with sealed classes)
  val themeFlow: Flow<Theme>
  val debugLoggingFlow: Flow<Boolean>
  val pluginUpdateCheckFlow: Flow<Boolean>
  val incomingCallActionFlow: Flow<CallAction>
  val libraryTrackDefaultActionFlow: Flow<TrackAction>
  val shouldDisplayOnlyArtists: Flow<Boolean>

  // Async update methods for changing settings (type-safe with sealed classes)
  suspend fun setTheme(theme: Theme)
  suspend fun setDebugLogging(enabled: Boolean)
  suspend fun setPluginUpdateCheck(enabled: Boolean)
  suspend fun setIncomingCallAction(action: CallAction)
  suspend fun setLibraryTrackDefaultAction(action: TrackAction)
  suspend fun setShouldDisplayOnlyAlbumArtist(onlyAlbumArtist: Boolean)

  // Async utility methods
  suspend fun checkShouldShowChangeLog(): Boolean
  suspend fun getLastUpdated(required: Boolean = false): Instant
  suspend fun setLastUpdated(lastChecked: Instant, required: Boolean = false)
}
