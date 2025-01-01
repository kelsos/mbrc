package com.kelsos.mbrc.features.playlists

import com.kelsos.mbrc.common.mvp.BasePresenter
import com.kelsos.mbrc.data.UserAction
import com.kelsos.mbrc.events.MessageEvent
import com.kelsos.mbrc.events.bus.RxBus
import com.kelsos.mbrc.networking.protocol.Protocol
import kotlinx.coroutines.launch

class PlaylistPresenterImpl(
  private val bus: RxBus,
  private val repository: PlaylistRepository,
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
    bus.post(MessageEvent.Companion.action(UserAction(Protocol.PLAYLIST_PLAY, path)))
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
