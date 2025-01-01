package com.kelsos.mbrc.features.lyrics

import android.os.Bundle
import android.widget.TextView
import androidx.constraintlayout.widget.Group
import androidx.recyclerview.widget.LinearLayoutManager
import com.kelsos.mbrc.BaseActivity
import com.kelsos.mbrc.R
import com.kelsos.mbrc.common.ui.EmptyRecyclerView
import org.koin.android.ext.android.inject

class LyricsActivity :
  BaseActivity(),
  LyricsView {
  private lateinit var lyricsRecycler: EmptyRecyclerView
  private lateinit var emptyView: Group
  private lateinit var emptyText: TextView
  private val presenter: LyricsPresenter by inject()

  private lateinit var adapter: LyricsAdapter

  public override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setContentView(R.layout.activity_lyrics)
    lyricsRecycler = findViewById(R.id.lyrics_recycler_view)
    emptyView = findViewById(R.id.empty_view)
    emptyText = findViewById(R.id.empty_view_text)

    super.setup()
    lyricsRecycler.setHasFixedSize(true)
    lyricsRecycler.emptyView = emptyView
    val layoutManager = LinearLayoutManager(this)
    lyricsRecycler.layoutManager = layoutManager
    adapter = LyricsAdapter()
    lyricsRecycler.adapter = adapter
  }

  override fun onStart() {
    super.onStart()
    presenter.attach(this)
    presenter.load()
  }

  override fun onStop() {
    super.onStop()
    presenter.detach()
  }

  override fun updateLyrics(lyrics: List<String>) {
    adapter.updateLyrics(lyrics)
  }

  override fun showNoLyrics() {
    emptyText.setText(R.string.no_lyrics)
    adapter.clear()
  }

  override fun active(): Int = R.id.nav_lyrics
}
