package com.kelsos.mbrc.common.state

import com.kelsos.mbrc.common.utilities.AppCoroutineDispatchers
import com.kelsos.mbrc.common.utilities.ScopeBase
import com.kelsos.mbrc.platform.mediasession.AppNotificationManager
import java.util.Timer
import kotlin.concurrent.fixedRateTimer
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import timber.log.Timber

@OptIn(FlowPreview::class)
class AppStateManager(
  private val appState: AppStatePublisher,
  private val connectionState: ConnectionStateFlow,
  private val notifications: AppNotificationManager,
  private val trackCache: PlayingTrackCache,
  dispatchers: AppCoroutineDispatchers
) : ScopeBase(dispatchers.io) {
  private var isRunning = false
  var timer: Timer? = null

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
        val currentPosition = playingPosition.map { it.current }.distinctUntilChanged().first()
        notifications.updateState(state, currentPosition)
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
      connectionState.connection.collect { connection ->
        notifications.connectionStateChanged(connection == ConnectionStatus.Connected)
      }
    }

    launch {
      playingPosition.collect { playingPosition ->
        val playerState = appState.playerStatus.map { it.state }.first()
        notifications.updateState(playerState, playingPosition.current)
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
    timer =
      fixedRateTimer("progress", period = UPDATE_PERIOD_MS) {
        updatePosition()
      }
  }

  private fun stopPositionUpdater() {
    timer?.cancel()
    timer?.purge()
  }

  private fun updatePosition() {
    launch {
      val playingPosition = appState.playingPosition
      val position = playingPosition.first()
      val current = (position.current + UPDATE_PERIOD_MS).coerceAtMost(position.total)
      if (current == position.current) {
        return@launch
      }
      appState.updatePlayingPosition(position.copy(current = current))
    }
  }

  companion object {
    private const val PLAYER_STATE_DEBOUNCE_MS = 600L
    private const val UPDATE_PERIOD_MS = 1000L
  }
}
