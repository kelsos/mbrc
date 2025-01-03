package com.kelsos.mbrc.features.minicontrol

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kelsos.mbrc.common.state.AppStateFlow
import com.kelsos.mbrc.common.state.PlayerState
import com.kelsos.mbrc.common.state.PlayingPosition
import com.kelsos.mbrc.common.state.PlayingTrack
import com.kelsos.mbrc.networking.protocol.UserActionUseCase
import com.kelsos.mbrc.networking.protocol.next
import com.kelsos.mbrc.networking.protocol.playPause
import com.kelsos.mbrc.networking.protocol.previous
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

class MiniControlViewModel(
  appState: AppStateFlow,
  private val userActionUseCase: UserActionUseCase,
) : ViewModel() {
  val state: Flow<MiniControlState> =
    combine(
      appState.playingTrack,
      appState.playingPosition,
      appState.playerStatus.map { it.state }.distinctUntilChanged(),
    ) { playingTrack, playingPosition, playerState ->
      MiniControlState(
        playingTrack = playingTrack,
        playingPosition = playingPosition,
        playingState = playerState,
      )
    }

  fun perform(action: MiniControlAction) {
    viewModelScope.launch {
      when (action) {
        MiniControlAction.PlayNext -> userActionUseCase.next()
        MiniControlAction.PlayPause -> userActionUseCase.playPause()
        MiniControlAction.PlayPrevious -> userActionUseCase.previous()
      }
    }
  }
}

data class MiniControlState(
  val playingTrack: PlayingTrack = PlayingTrack(),
  val playingPosition: PlayingPosition = PlayingPosition(),
  val playingState: PlayerState = PlayerState.Undefined,
)
