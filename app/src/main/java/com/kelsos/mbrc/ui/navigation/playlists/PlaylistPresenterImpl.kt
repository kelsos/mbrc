package com.kelsos.mbrc.ui.navigation.playlists

import com.kelsos.mbrc.constants.Protocol
import com.kelsos.mbrc.data.UserAction
import com.kelsos.mbrc.events.MessageEvent
import com.kelsos.mbrc.events.bus.RxBus
import com.kelsos.mbrc.mvp.BasePresenter
import com.kelsos.mbrc.repository.PlaylistRepository
import kotlinx.coroutines.launch
import javax.inject.Inject

class PlaylistPresenterImpl
@Inject
constructor(
  private val bus: RxBus,
  private val repository: PlaylistRepository
) : BasePresenter<PlaylistView>(),
  PlaylistPresenter {

  override fun load() {
    scope.launch {
      try {
        view?.update(repository.getAllCursor())
      } catch (e: Exception) {
        view?.failure(e)
      }
    }
  }

  override fun play(path: String) {
    bus.post(MessageEvent.action(UserAction(Protocol.PlaylistPlay, path)))
  }

  override fun reload() {
    scope.launch {
      try {
        view?.update(repository.getAndSaveRemote())
      } catch (e: Exception) {
        view?.failure(e)
      }
    }
  }
}
