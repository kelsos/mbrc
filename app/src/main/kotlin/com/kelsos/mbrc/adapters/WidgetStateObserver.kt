package com.kelsos.mbrc.adapters

import com.kelsos.mbrc.core.common.state.AppStateFlow
import com.kelsos.mbrc.core.common.state.PlayerState
import com.kelsos.mbrc.core.common.utilities.coroutines.AppCoroutineDispatchers
import com.kelsos.mbrc.core.platform.state.toPlayingTrack
import com.kelsos.mbrc.feature.widgets.WidgetUpdater
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
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
      appState.playingTrack
        // Both flows replay what they are already holding, which on a fresh process is the
        // uninitialized value: an empty track would blank the widget's text and, because a blank
        // cover URL deletes the cached bitmap, take its artwork with it. The widget persists its
        // own content, so it is better left alone until there is something real to say. The state
        // is only populated from the cache by AppStateManager, which nothing resolves until the
        // service starts, so this is every cold start rather than an edge case.
        .filter { it.title.isNotEmpty() || it.artist.isNotEmpty() }
        .map { it.toPlayingTrack() }
        // Only what the widget draws. Duration and path changes arrive as their own updates and
        // would otherwise each rewrite every widget instance for nothing.
        .distinctUntilChanged { previous, current ->
          previous.title == current.title &&
            previous.artist == current.artist &&
            previous.album == current.album &&
            previous.coverUrl == current.coverUrl
        }
        .collect(widgetUpdater::updatePlayingTrack)
    }

    scope.launch(dispatchers.io) {
      appState.playerStatus
        .map { it.state }
        // Undefined is the initial value, and the widget renders anything that is not Playing as
        // paused, so letting it through flips a playing widget to paused on every process start.
        .filter { it != PlayerState.Undefined }
        .distinctUntilChanged()
        .collect(widgetUpdater::updatePlayState)
    }
  }
}
