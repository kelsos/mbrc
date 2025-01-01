package com.kelsos.mbrc.features.nowplaying

import com.kelsos.mbrc.common.mvp.BasePresenter
import com.kelsos.mbrc.common.state.MainDataModel
import com.kelsos.mbrc.data.UserAction
import com.kelsos.mbrc.events.MessageEvent
import com.kelsos.mbrc.events.bus.RxBus
import com.kelsos.mbrc.events.ui.TrackInfoChangeEvent
import com.kelsos.mbrc.networking.protocol.NowPlayingMoveRequest
import com.kelsos.mbrc.networking.protocol.Protocol
import kotlinx.coroutines.launch

class NowPlayingPresenterImpl(
  private val repository: NowPlayingRepository,
  private val bus: RxBus,
  private val model: MainDataModel,
) : BasePresenter<NowPlayingView>(),
  NowPlayingPresenter {
  override fun reload(scrollToTrack: Boolean) {
    scope.launch {
      try {
        view?.update(repository.getAndSaveRemote())
        view?.trackChanged(model.trackInfo, scrollToTrack)
      } catch (e: Exception) {
        view?.failure(e)
      }
    }
  }

  override fun load() {
    scope.launch {
      try {
        view?.update(repository.getAllCursor())
        view?.trackChanged(model.trackInfo, true)
        view?.loading()
        reload(true)
      } catch (e: Exception) {
        view?.failure(e)
      }
    }
  }

  override fun search(query: String) {
    bus.post(
      MessageEvent.action(
        UserAction(
          Protocol.NOW_PLAYING_LIST_SEARCH,
          query.trim { it <= ' ' },
        ),
      ),
    )
  }

  override fun moveTrack(
    from: Int,
    to: Int,
  ) {
    val data = NowPlayingMoveRequest(from, to)
    bus.post(MessageEvent.action(UserAction(Protocol.NOW_PLAYING_LIST_MOVE, data)))
  }

  override fun play(position: Int) {
    bus.post(MessageEvent.action(UserAction(Protocol.NOW_PLAYING_LIST_PLAY, position)))
  }

  override fun attach(view: NowPlayingView) {
    super.attach(view)
    bus.register(
      this,
      TrackInfoChangeEvent::class.java,
      { this.view?.trackChanged(it.trackInfo) },
      true,
    )
  }

  override fun detach() {
    super.detach()
    bus.unregister(this)
  }

  override fun removeTrack(position: Int) {
    bus.post(MessageEvent.action(UserAction(Protocol.NOW_PLAYING_LIST_REMOVE, position)))
  }
}
