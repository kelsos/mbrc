package com.kelsos.mbrc.features.nowplaying

import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import androidx.paging.cachedIn
import com.kelsos.mbrc.common.mvvm.BaseViewModel
import com.kelsos.mbrc.common.state.AppStateFlow
import com.kelsos.mbrc.common.state.ConnectionStateFlow
import com.kelsos.mbrc.common.utilities.AppCoroutineDispatchers
import com.kelsos.mbrc.networking.protocol.NowPlayingMoveRequest
import com.kelsos.mbrc.networking.protocol.UserActionUseCase
import com.kelsos.mbrc.networking.protocol.moveTrack
import com.kelsos.mbrc.networking.protocol.playTrack
import com.kelsos.mbrc.networking.protocol.removeTrack
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import java.io.IOException

class NowPlayingViewModel(
  private val dispatchers: AppCoroutineDispatchers,
  private val repository: NowPlayingRepository,
  private val moveManager: MoveManager,
  private val userActionUseCase: UserActionUseCase,
  private val connectionState: ConnectionStateFlow,
  appState: AppStateFlow,
) : BaseViewModel<NowPlayingUiMessages>() {
  val tracks: Flow<PagingData<NowPlaying>> = repository.getAll().cachedIn(viewModelScope)
  val playingTrack = appState.playingTrack

  init {
    moveManager.onMoveCommit { originalPosition, finalPosition ->
      viewModelScope.launch(dispatchers.network) {
        userActionUseCase.moveTrack(NowPlayingMoveRequest(originalPosition, finalPosition))
      }
    }
  }

  fun reload() {
    viewModelScope.launch(dispatchers.network) {
      if (!connectionState.isConnected()) {
        return@launch
      }
      val result =
        try {
          repository.getRemote()
          NowPlayingUiMessages.RefreshSucceeded
        } catch (e: IOException) {
          NowPlayingUiMessages.RefreshFailed(e)
        }

      emit(result)
    }
  }

  fun search(query: String) {
    viewModelScope.launch(dispatchers.database) {
      val position = repository.findPosition(query)
      if (position > 0) {
        play(position)
      }
    }
  }

  fun moveTrack(
    from: Int,
    to: Int,
  ) {
    moveManager.move(from, to)
  }

  fun play(position: Int) {
    viewModelScope.launch(dispatchers.network) {
      userActionUseCase.playTrack(position)
    }
  }

  fun removeTrack(position: Int) {
    viewModelScope.launch(dispatchers.network) {
      delay(REMOVE_DELAY_MS)
      userActionUseCase.removeTrack(position)
    }
  }

  fun move() {
    moveManager.commit()
  }

  companion object {
    const val REMOVE_DELAY_MS = 400L
  }
}
