package com.kelsos.mbrc.features.playlists

import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import androidx.paging.cachedIn
import com.kelsos.mbrc.common.mvvm.BaseViewModel
import com.kelsos.mbrc.common.mvvm.UiMessageBase
import com.kelsos.mbrc.common.utilities.AppCoroutineDispatchers
import com.kelsos.mbrc.networking.protocol.Protocol
import com.kelsos.mbrc.networking.protocol.UserActionUseCase
import com.kelsos.mbrc.networking.protocol.performUserAction
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import timber.log.Timber
import java.io.IOException

sealed class PlaylistUiMessages : UiMessageBase {
  object RefreshFailed : PlaylistUiMessages()

  object RefreshSuccess : PlaylistUiMessages()
}

interface IPlaylistActions {
  fun play(path: String)

  fun reload()
}

class PlaylistActions(
  private val scope: CoroutineScope,
  private val dispatchers: AppCoroutineDispatchers,
  private val repository: PlaylistRepository,
  private val userActionUseCase: UserActionUseCase,
  private val emit: suspend (uiMessage: PlaylistUiMessages) -> Unit,
) : IPlaylistActions {
  override fun play(path: String) {
    scope.launch(dispatchers.network) {
      userActionUseCase.performUserAction(Protocol.PlaylistPlay, path)
    }
  }

  override fun reload() {
    scope.launch(dispatchers.network) {
      val result =
        try {
          repository.getRemote()
          PlaylistUiMessages.RefreshSuccess
        } catch (e: IOException) {
          Timber.e(e)
          PlaylistUiMessages.RefreshFailed
        }
      emit(result)
    }
  }
}

class PlaylistViewModel(
  repository: PlaylistRepository,
  dispatchers: AppCoroutineDispatchers,
  userActionsUseCase: UserActionUseCase,
) : BaseViewModel<PlaylistUiMessages>() {
  val playlists: Flow<PagingData<Playlist>> = repository.getAll().cachedIn(viewModelScope)
  val actions: PlaylistActions =
    PlaylistActions(
      scope = viewModelScope,
      dispatchers = dispatchers,
      repository = repository,
      userActionUseCase = userActionsUseCase,
      emit = this::emit,
    )
}
