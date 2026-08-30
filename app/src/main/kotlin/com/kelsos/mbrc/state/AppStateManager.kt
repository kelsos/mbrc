package com.kelsos.mbrc.state

import com.kelsos.mbrc.core.common.state.AppStatePublisher
import com.kelsos.mbrc.core.common.state.ConnectionStateFlow
import com.kelsos.mbrc.core.common.state.ConnectionStatus
import com.kelsos.mbrc.core.common.state.PlayerState
import com.kelsos.mbrc.core.common.utilities.coroutines.AppCoroutineDispatchers
import com.kelsos.mbrc.core.common.utilities.coroutines.ScopeBase
import com.kelsos.mbrc.service.ServiceLifecycleManager
import com.kelsos.mbrc.service.mediasession.AppNotificationManager
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import timber.log.Timber

@OptIn(FlowPreview::class)
class AppStateManager(
  private val appState: AppStatePublisher,
  private val connectionState: ConnectionStateFlow,
  private val notifications: AppNotificationManager,
  private val trackCache: PlayingTrackCache,
  private val serviceLifecycleManager: ServiceLifecycleManager,
  dispatchers: AppCoroutineDispatchers
) : ScopeBase(dispatchers.io) {
  private var isRunning = false
  private var positionJob: Job? = null

  init {
    launch {
      val track = trackCache.restoreInfo()
      Timber.v("Restoring playing last played track: $track")
      appState.updatePlayingTrack(track)
    }
  }

  fun start() {
    if (isRunning) {
      Timber.v("state manager is already running")
      return
    }

    this.onStart()
    isRunning = true

    val playingPosition = appState.playingPosition
    val debouncedPlayerState =
      appState.playerStatus
        .map { it.state }
        .distinctUntilChanged { old, new -> old == new }
        .debounce(PLAYER_STATE_DEBOUNCE_MS)

    launch {
      debouncedPlayerState.collect { state ->
        val position = playingPosition.first()
        notifications.updateState(state, position)
        if (state == PlayerState.Playing) {
          startPositionUpdater()
        } else {
          stopPositionUpdater()
        }
      }
    }

    launch {
      appState.playingTrack.collect { playingTrack ->
        notifications.updatePlayingTrack(playingTrack)
        trackCache.persistInfo(playingTrack)
      }
    }

    launch {
      // Track if a connection was ever attempted to avoid triggering
      // reconnection logic on the initial Offline state (which is the default)
      var wasConnectionAttempted = false

      connectionState.connection.collect { connection ->
        notifications.connectionStateChanged(connection == ConnectionStatus.Connected)
        when (connection) {
          ConnectionStatus.Offline -> {
            stopPositionUpdater()
            if (wasConnectionAttempted) {
              serviceLifecycleManager.onConnectionLost()
            }
          }

          ConnectionStatus.Connected -> {
            wasConnectionAttempted = true
            serviceLifecycleManager.onConnectionRestored()
          }

          ConnectionStatus.Authenticating -> {
            wasConnectionAttempted = true
          }

          is ConnectionStatus.Connecting -> {
            wasConnectionAttempted = true
          }

          ConnectionStatus.LocalNetworkDenied -> {
            // Not a connection loss: retrying cannot help, so do not hand this to the
            // reconnection loop.
            stopPositionUpdater()
          }
        }
      }
    }

    launch {
      playingPosition.collect { position ->
        val playerState = appState.playerStatus.map { it.state }.first()
        notifications.updateState(playerState, position)
      }
    }
  }

  fun stop() {
    this.onStop()
    notifications.cancel()
    stopPositionUpdater()
    isRunning = false
  }

  private fun startPositionUpdater() {
    stopPositionUpdater()
    positionJob = launch {
      while (isActive) {
        delay(UPDATE_PERIOD_MS)
        updatePosition()
      }
    }
  }

  private fun stopPositionUpdater() {
    positionJob?.cancel()
    positionJob = null
  }

  private fun updatePosition() {
    val position = appState.playingPosition.value
    // For streams (total <= 0), don't coerce - just increment the elapsed time
    val current = if (position.total <= 0) {
      position.current + UPDATE_PERIOD_MS
    } else {
      (position.current + UPDATE_PERIOD_MS).coerceAtMost(position.total)
    }
    if (current != position.current) {
      appState.updatePlayingPosition(position.copy(current = current))
    }
  }

  companion object {
    private const val PLAYER_STATE_DEBOUNCE_MS = 600L
    private const val UPDATE_PERIOD_MS = 1000L
  }
}
