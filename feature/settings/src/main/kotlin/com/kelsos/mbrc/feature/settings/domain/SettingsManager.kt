package com.kelsos.mbrc.feature.settings.domain

import com.kelsos.mbrc.core.common.settings.ChangeLogChecker
import com.kelsos.mbrc.core.common.settings.LibrarySettings
import com.kelsos.mbrc.core.common.settings.TrackAction
import com.kelsos.mbrc.feature.settings.data.CallAction
import com.kelsos.mbrc.feature.settings.data.KeepScreenOn
import com.kelsos.mbrc.feature.settings.theme.Theme
import java.time.Instant
import kotlinx.coroutines.flow.Flow

/**
 * Modern reactive and async settings manager interface.
 * All settings are exposed as reactive flows and updates are async.
 * Extends LibrarySettings and ChangeLogChecker for use by feature modules.
 */
interface SettingsManager :
  LibrarySettings,
  ChangeLogChecker {
  // Reactive flows for observing all settings (type-safe with sealed classes)
  val themeFlow: Flow<Theme>
  val debugLoggingFlow: Flow<Boolean>
  val pluginUpdateCheckFlow: Flow<Boolean>
  val incomingCallActionFlow: Flow<CallAction>
  override val libraryTrackDefaultActionFlow: Flow<TrackAction>
  override val shouldDisplayOnlyArtists: Flow<Boolean>
  val halfStarRatingFlow: Flow<Boolean>
  val showRatingOnPlayerFlow: Flow<Boolean>
  val keepScreenOnFlow: Flow<KeepScreenOn>

  // Async update methods for changing settings (type-safe with sealed classes)
  suspend fun setTheme(theme: Theme)
  suspend fun setDebugLogging(enabled: Boolean)
  suspend fun setPluginUpdateCheck(enabled: Boolean)
  suspend fun setIncomingCallAction(action: CallAction)
  suspend fun setLibraryTrackDefaultAction(action: TrackAction)
  override suspend fun setShouldDisplayOnlyAlbumArtist(onlyAlbumArtist: Boolean)
  suspend fun setHalfStarRating(enabled: Boolean)
  suspend fun setShowRatingOnPlayer(enabled: Boolean)
  suspend fun setKeepScreenOn(mode: KeepScreenOn)

  // Async utility methods
  override suspend fun checkShouldShowChangeLog(): Boolean
  suspend fun getLastUpdated(required: Boolean = false): Instant
  suspend fun setLastUpdated(lastChecked: Instant, required: Boolean = false)
}
