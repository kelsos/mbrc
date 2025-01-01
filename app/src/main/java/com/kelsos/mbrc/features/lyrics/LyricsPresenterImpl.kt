package com.kelsos.mbrc.features.lyrics

import com.kelsos.mbrc.common.mvp.BasePresenter
import com.kelsos.mbrc.constants.Const
import com.kelsos.mbrc.events.bus.RxBus
import com.kelsos.mbrc.events.ui.LyricsUpdatedEvent

class LyricsPresenterImpl(
  private val bus: RxBus,
  private val model: LyricsModel,
) : BasePresenter<LyricsView>(),
  LyricsPresenter {
  override fun attach(view: LyricsView) {
    super.attach(view)
    bus.register(this, LyricsUpdatedEvent::class.java, { load() }, true)
  }

  override fun detach() {
    super.detach()
    bus.unregister(this)
  }

  override fun load() {
    if (!isAttached) {
      return
    }

    if (model.status == LyricsPayload.Companion.NOT_FOUND) {
      view?.showNoLyrics()
    } else {
      updateLyrics(model.lyrics)
    }
  }

  fun updateLyrics(text: String) {
    if (!isAttached) {
      return
    }
    val lyrics =
      ArrayList(
        listOf<String>(
          *text
            .split(Const.LYRICS_NEWLINE.toRegex())
            .dropLastWhile(String::isEmpty)
            .toTypedArray(),
        ),
      )
    view?.updateLyrics(lyrics)
  }
}
