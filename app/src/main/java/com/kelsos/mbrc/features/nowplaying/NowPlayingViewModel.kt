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
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import timber.log.Timber
import java.io.IOException

interface INowPlayingActions {
  fun reload()

  fun reload(showUserMessage: Boolean)

  fun play(position: Int)

  fun removeTrack(position: Int)

  fun moveTrack(
    from: Int,
    to: Int,
  )

  fun move()

  fun search(query: String)
}

class NowPlayingActions(
  private val scope: CoroutineScope,
  private val dispatchers: AppCoroutineDispatchers,
  private val repository: NowPlayingRepository,
  private val moveManager: MoveManager,
  private val userActionUseCase: UserActionUseCase,
  private val connectionStateFlow: ConnectionStateFlow,
  private val emit: suspend (uiMessage: NowPlayingUiMessages) -> Unit,
) : INowPlayingActions {
  override fun reload() {
    reload(showUserMessage = true)
  }

  override fun reload(showUserMessage: Boolean) {
    scope.launch(dispatchers.network) {
      if (!connectionStateFlow.isConnected()) {
        if (showUserMessage) {
          emit(NowPlayingUiMessages.NetworkUnavailable)
        }
        return@launch
      }
      val result =
        try {
          repository.getRemote()
          if (showUserMessage) NowPlayingUiMessages.RefreshSucceeded else null
        } catch (e: IOException) {
          Timber.e(e)
          if (showUserMessage) NowPlayingUiMessages.RefreshFailed(e) else null
        }

      result?.let { emit(it) }
    }
  }

  override fun play(position: Int) {
    scope.launch(dispatchers.network) {
      if (!connectionStateFlow.isConnected()) {
        emit(NowPlayingUiMessages.NetworkUnavailable)
        return@launch
      }
      try {
        userActionUseCase.playTrack(position)
      } catch (e: IOException) {
        Timber.e(e)
        emit(NowPlayingUiMessages.PlayFailed)
      }
    }
  }

  override fun removeTrack(position: Int) {
    scope.launch(dispatchers.network) {
      if (!connectionStateFlow.isConnected()) {
        emit(NowPlayingUiMessages.NetworkUnavailable)
        return@launch
      }
      try {
        delay(REMOVE_DELAY_MS)
        userActionUseCase.removeTrack(position)
      } catch (e: IOException) {
        Timber.e(e)
        emit(NowPlayingUiMessages.RemoveFailed)
      }
    }
  }

  override fun moveTrack(
    from: Int,
    to: Int,
  ) {
    moveManager.move(from, to)
  }

  override fun move() {
    scope.launch(dispatchers.network) {
      if (!connectionStateFlow.isConnected()) {
        emit(NowPlayingUiMessages.NetworkUnavailable)
        return@launch
      }
      moveManager.commit()
    }
  }

  override fun search(query: String) {
    scope.launch(dispatchers.database) {
      val position = repository.findPosition(query)
      if (position > 0) {
        play(position)
      }
    }
  }

  companion object {
    const val REMOVE_DELAY_MS = 400L
  }
}

class NowPlayingViewModel(
  repository: NowPlayingRepository,
  dispatchers: AppCoroutineDispatchers,
  moveManager: MoveManager,
  userActionUseCase: UserActionUseCase,
  connectionStateFlow: ConnectionStateFlow,
  appState: AppStateFlow,
) : BaseViewModel<NowPlayingUiMessages>() {
  val tracks: Flow<PagingData<NowPlaying>> = repository.getAll().cachedIn(viewModelScope)
  val playingTrack = appState.playingTrack
  val connectionState = connectionStateFlow.connection
  val actions: NowPlayingActions =
    NowPlayingActions(
      scope = viewModelScope,
      dispatchers = dispatchers,
      repository = repository,
      moveManager = moveManager,
      userActionUseCase = userActionUseCase,
      connectionStateFlow = connectionStateFlow,
      emit = this::emit,
    )

  init {
    moveManager.onMoveCommit { originalPosition, finalPosition ->
      viewModelScope.launch(dispatchers.network) {
        try {
          userActionUseCase.moveTrack(NowPlayingMoveRequest(originalPosition, finalPosition))
          repository.move(originalPosition + 1, finalPosition + 1)
        } catch (e: IOException) {
          Timber.e(e)
          emit(NowPlayingUiMessages.MoveFailed)
        }
      }
    }
  }
}
