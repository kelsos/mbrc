package com.kelsos.mbrc.features.player

import androidx.lifecycle.viewModelScope
import com.kelsos.mbrc.common.mvvm.BaseViewModel
import com.kelsos.mbrc.common.state.AppStateFlow
import com.kelsos.mbrc.common.state.PlayerStatusModel
import com.kelsos.mbrc.common.state.PlayingPosition
import com.kelsos.mbrc.common.state.PlayingTrack
import com.kelsos.mbrc.common.state.TrackRating
import com.kelsos.mbrc.features.settings.SettingsManager
import com.kelsos.mbrc.networking.protocol.Protocol
import com.kelsos.mbrc.networking.protocol.UserAction
import com.kelsos.mbrc.networking.protocol.UserActionUseCase
import com.kelsos.mbrc.networking.protocol.performUserAction
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.sample
import kotlinx.coroutines.launch

@OptIn(FlowPreview::class)
class PlayerViewModel(
  settingsManager: SettingsManager,
  appState: AppStateFlow,
  private val userActionUseCase: UserActionUseCase
) : BaseViewModel<PlayerUiMessage>() {
  private val progressRelay: MutableSharedFlow<Int> = MutableSharedFlow()
  private val volumeRelay: MutableSharedFlow<Int> = MutableSharedFlow()

  val state: Flow<PlayerStateModel> =
    combine(
      appState.playingTrack,
      appState.playerStatus,
      appState.playingTrackRating,
      appState.playingPosition
    ) { playingTrack, playerStatus, trackRating, playingPosition ->
      PlayerStateModel(
        playingTrack = playingTrack,
        playerStatus = playerStatus,
        trackRating = trackRating,
        playingPosition = playingPosition
      )
    }

  init {
    viewModelScope.launch {
      progressRelay.sample(ACTION_DEBOUNCE_MS).collect { position ->
        userActionUseCase.performUserAction(Protocol.NowPlayingPosition, position)
      }
    }

    viewModelScope.launch {
      volumeRelay.sample(ACTION_DEBOUNCE_MS).collect { volume ->
        userActionUseCase.performUserAction(Protocol.PlayerVolume, volume)
      }
    }

    viewModelScope.launch {
      if (settingsManager.shouldShowChangeLog()) {
        emit(PlayerUiMessage.ShowChangelog)
      }
    }
  }

  fun interact(action: PlayerAction) {
    if (action is PlayerAction.Seek) {
      viewModelScope.launch {
        progressRelay.emit(action.position)
      }
      return
    }

    if (action is PlayerAction.ChangeVolume) {
      viewModelScope.launch {
        volumeRelay.emit(action.volume)
      }
      return
    }

    val userAction =
      when (action) {
        PlayerAction.ToggleFavorite -> UserAction.toggle(Protocol.NowPlayingLfmRating)
        PlayerAction.PlayNext -> UserAction(Protocol.PlayerNext, true)
        PlayerAction.ResumePlayOrPause -> UserAction(Protocol.PlayerPlayPause, true)
        PlayerAction.PlayPrevious -> UserAction(Protocol.PlayerPrevious, true)
        PlayerAction.ToggleRepeat -> UserAction.toggle(Protocol.PlayerRepeat)
        PlayerAction.ToggleScrobbling -> UserAction.toggle(Protocol.PlayerScrobble)
        PlayerAction.ToggleShuffle -> UserAction.toggle(Protocol.PlayerShuffle)
        PlayerAction.Stop -> UserAction(Protocol.PlayerStop, true)
        PlayerAction.ToggleMute -> UserAction.toggle(Protocol.PlayerMute)
        is PlayerAction.Seek -> throw IllegalArgumentException("Handled before")
        is PlayerAction.ChangeVolume -> throw IllegalArgumentException("Handled before")
      }

    viewModelScope.launch {
      userActionUseCase.perform(userAction)
    }
  }

  companion object {
    private const val ACTION_DEBOUNCE_MS = 800L
  }
}

data class PlayerStateModel(
  val playingTrack: PlayingTrack = PlayingTrack(),
  val playingPosition: PlayingPosition = PlayingPosition(),
  val playerStatus: PlayerStatusModel = PlayerStatusModel(),
  val trackRating: TrackRating = TrackRating()
)
