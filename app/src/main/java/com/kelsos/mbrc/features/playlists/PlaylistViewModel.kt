package com.kelsos.mbrc.features.playlists

import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import androidx.paging.cachedIn
import com.kelsos.mbrc.common.mvvm.BaseViewModel
import com.kelsos.mbrc.common.mvvm.UiMessageBase
import com.kelsos.mbrc.common.state.ConnectionStateFlow
import com.kelsos.mbrc.common.utilities.AppCoroutineDispatchers
import com.kelsos.mbrc.networking.protocol.Protocol
import com.kelsos.mbrc.networking.protocol.UserActionUseCase
import com.kelsos.mbrc.networking.protocol.performUserAction
import java.io.IOException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import timber.log.Timber

sealed class PlaylistUiMessages : UiMessageBase {
  object RefreshFailed : PlaylistUiMessages()

  object RefreshSuccess : PlaylistUiMessages()

  object NetworkUnavailable : PlaylistUiMessages()

  object PlayFailed : PlaylistUiMessages()
}

interface IPlaylistActions {
  fun play(path: String)

  fun reload()

  fun reload(showUserMessage: Boolean)
}

class PlaylistActions(
  private val scope: CoroutineScope,
  private val dispatchers: AppCoroutineDispatchers,
  private val repository: PlaylistRepository,
  private val userActionUseCase: UserActionUseCase,
  private val connectionStateFlow: ConnectionStateFlow,
  private val emit: suspend (uiMessage: PlaylistUiMessages) -> Unit
) : IPlaylistActions {
  override fun play(path: String) {
    scope.launch(dispatchers.network) {
      val result =
        if (!connectionStateFlow.isConnected()) {
          PlaylistUiMessages.NetworkUnavailable
        } else {
          try {
            userActionUseCase.performUserAction(Protocol.PlaylistPlay, path)
            return@launch // Don't emit anything on success for play action
          } catch (e: IOException) {
            Timber.e(e)
            PlaylistUiMessages.PlayFailed
          }
        }
      emit(result)
    }
  }

  override fun reload() {
    reload(showUserMessage = true)
  }

  override fun reload(showUserMessage: Boolean) {
    scope.launch(dispatchers.network) {
      if (!connectionStateFlow.isConnected()) {
        if (showUserMessage) {
          emit(PlaylistUiMessages.NetworkUnavailable)
        }
        return@launch
      }

      val result =
        try {
          repository.getRemote()
          if (showUserMessage) PlaylistUiMessages.RefreshSuccess else null
        } catch (e: IOException) {
          Timber.e(e)
          if (showUserMessage) PlaylistUiMessages.RefreshFailed else null
        }

      result?.let { emit(it) }
    }
  }
}

class PlaylistViewModel(
  repository: PlaylistRepository,
  dispatchers: AppCoroutineDispatchers,
  userActionsUseCase: UserActionUseCase,
  connectionStateFlow: ConnectionStateFlow
) : BaseViewModel<PlaylistUiMessages>() {
  val playlists: Flow<PagingData<Playlist>> = repository.getAll().cachedIn(viewModelScope)
  val actions: PlaylistActions =
    PlaylistActions(
      scope = viewModelScope,
      dispatchers = dispatchers,
      repository = repository,
      userActionUseCase = userActionsUseCase,
      connectionStateFlow = connectionStateFlow,
      emit = this::emit
    )
}
