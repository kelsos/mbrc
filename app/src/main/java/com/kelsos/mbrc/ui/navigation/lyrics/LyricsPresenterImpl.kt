package com.kelsos.mbrc.ui.navigation.lyrics

import com.kelsos.mbrc.constants.Const
import com.kelsos.mbrc.data.LyricsPayload
import com.kelsos.mbrc.events.bus.RxBus
import com.kelsos.mbrc.events.ui.LyricsUpdatedEvent
import com.kelsos.mbrc.model.LyricsModel
import com.kelsos.mbrc.mvp.BasePresenter
import java.util.*
import javax.inject.Inject

class LyricsPresenterImpl
  @Inject
  constructor(
    private val bus: RxBus,
  ) : BasePresenter<LyricsView>(),
    LyricsPresenter {
    @Inject
    lateinit var model: LyricsModel

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

      if (model.status == LyricsPayload.NOT_FOUND) {
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
          Arrays.asList<String>(
            *text
              .split(Const.LYRICS_NEWLINE.toRegex())
              .dropLastWhile(String::isEmpty)
              .toTypedArray(),
          ),
        )
      view?.updateLyrics(lyrics)
    }
  }
