package com.kelsos.mbrc.features.lyrics

import com.kelsos.mbrc.common.mvp.BaseView
import com.kelsos.mbrc.common.mvp.Presenter

interface LyricsView : BaseView {
  fun updateLyrics(lyrics: List<String>)

  fun showNoLyrics()
}

interface LyricsPresenter : Presenter<LyricsView> {
  fun load()
}
