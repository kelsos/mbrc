package com.kelsos.mbrc.feature.playback.nowplaying

import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import androidx.paging.cachedIn
import com.kelsos.mbrc.core.common.mvvm.BaseViewModel
import com.kelsos.mbrc.core.common.state.AppStateFlow
import com.kelsos.mbrc.core.common.state.ConnectionStateFlow
import com.kelsos.mbrc.core.common.utilities.coroutines.AppCoroutineDispatchers
import com.kelsos.mbrc.core.data.nowplaying.NowPlaying
import com.kelsos.mbrc.core.networking.protocol.payloads.NowPlayingMoveRequest
import com.kelsos.mbrc.core.networking.protocol.usecases.UserActionUseCase
import com.kelsos.mbrc.core.networking.protocol.usecases.moveTrack
import com.kelsos.mbrc.core.networking.protocol.usecases.playTrack
import com.kelsos.mbrc.core.networking.protocol.usecases.removeTrack
import java.io.IOException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import timber.log.Timber

interface INowPlayingActions {
  fun reload()

  fun reload(showUserMessage: Boolean)

  fun play(position: Int)

  fun removeTrack(position: Int)

  fun moveTrack(from: Int, to: Int)

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
  private val emit: suspend (uiMessage: NowPlayingUiMessages) -> Unit
) : INowPlayingActions {
  override fun reload() {
    reload(showUserMessage = true)
  }

  override fun reload(showUserMessage: Boolean) {
    scope.launch(dispatchers.network) {
      if (!connectionStateFlow.isConnected) {
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
      if (!connectionStateFlow.isConnected) {
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
      if (!connectionStateFlow.isConnected) {
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

  override fun moveTrack(from: Int, to: Int) {
    moveManager.move(from, to)
  }

  override fun move() {
    scope.launch(dispatchers.network) {
      if (!connectionStateFlow.isConnected) {
        emit(NowPlayingUiMessages.NetworkUnavailable)
        return@launch
      }
      moveManager.commit()
    }
  }

  override fun search(query: String) {
    scope.launch(dispatchers.database) {
      val result = repository.searchTrack(query)
      if (result == null || result.position <= 0) {
        emit(NowPlayingUiMessages.SearchNotFound)
        return@launch
      }

      // Check connection before playing
      if (!connectionStateFlow.isConnected) {
        emit(NowPlayingUiMessages.NetworkUnavailable)
        return@launch
      }

      try {
        userActionUseCase.playTrack(result.position)
        emit(NowPlayingUiMessages.SearchSuccess(result.title))
      } catch (e: IOException) {
        Timber.e(e)
        emit(NowPlayingUiMessages.PlayFailed)
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
  appState: AppStateFlow
) : BaseViewModel<NowPlayingUiMessages>() {
  val tracks: Flow<PagingData<NowPlaying>> = repository.getAll().cachedIn(viewModelScope)
  val playingTrack = appState.playingTrack
  val connectionState = connectionStateFlow.connection
  val trackCount: StateFlow<Int> = repository.observeCount()
    .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)
  val actions: NowPlayingActions =
    NowPlayingActions(
      scope = viewModelScope,
      dispatchers = dispatchers,
      repository = repository,
      moveManager = moveManager,
      userActionUseCase = userActionUseCase,
      connectionStateFlow = connectionStateFlow,
      emit = this::emit
    )

  init {
    actions.reload(showUserMessage = false)
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
