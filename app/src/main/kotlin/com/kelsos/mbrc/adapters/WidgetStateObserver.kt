package com.kelsos.mbrc.adapters

import com.kelsos.mbrc.core.common.state.AppStateFlow
import com.kelsos.mbrc.core.common.utilities.coroutines.AppCoroutineDispatchers
import com.kelsos.mbrc.core.platform.state.toPlayingTrack
import com.kelsos.mbrc.feature.widgets.WidgetUpdater
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

/**
 * Drives the widget from the same state the in-app UI reads.
 *
 * The widget used to be updated from the track-change callback alone, which fires while the cover
 * for the new track is still on its way: the plugin sends the metadata and the artwork as two
 * separate messages. The callback therefore handed the widget each new title paired with the
 * previous track's cover, and nothing corrected it when the artwork arrived, because the cover
 * message only ever updated [AppStateFlow] (issue 369).
 *
 * Collecting the state instead means the widget is told about both messages, and there is no longer
 * a second update path that can drift from the first.
 */
class WidgetStateObserver(
  private val appState: AppStateFlow,
  private val widgetUpdater: WidgetUpdater,
  private val dispatchers: AppCoroutineDispatchers
) {
  fun start(scope: CoroutineScope) {
    scope.launch(dispatchers.io) {
      appState.playingTrack.collect { track ->
        widgetUpdater.updatePlayingTrack(track.toPlayingTrack())
      }
    }

    scope.launch(dispatchers.io) {
      appState.playerStatus
        .map { it.state }
        .distinctUntilChanged()
        .collect(widgetUpdater::updatePlayState)
    }
  }
}
