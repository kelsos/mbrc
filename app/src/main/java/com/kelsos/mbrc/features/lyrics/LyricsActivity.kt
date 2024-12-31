package com.kelsos.mbrc.features.lyrics

import android.os.Bundle
import android.widget.TextView
import androidx.constraintlayout.widget.Group
import androidx.recyclerview.widget.LinearLayoutManager
import com.kelsos.mbrc.BaseActivity
import com.kelsos.mbrc.R
import com.kelsos.mbrc.common.ui.EmptyRecyclerView
import toothpick.Scope
import toothpick.Toothpick
import toothpick.smoothie.module.SmoothieActivityModule
import javax.inject.Inject

class LyricsActivity :
  BaseActivity(),
  LyricsView {
  private val presenterScope: Class<*> = Presenter::class.java

  private lateinit var lyricsRecycler: EmptyRecyclerView
  private lateinit var emptyView: Group
  private lateinit var emptyText: TextView

  @Inject
  lateinit var presenter: LyricsPresenter

  private lateinit var scope: Scope
  private lateinit var adapter: LyricsAdapter

  public override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setContentView(R.layout.activity_lyrics)
    lyricsRecycler = findViewById(R.id.lyrics_recycler_view)
    emptyView = findViewById(R.id.empty_view)
    emptyText = findViewById(R.id.empty_view_text)

    scope = Toothpick.openScopes(application, presenterScope, this)
    scope.installModules(SmoothieActivityModule(this), LyricsModule())
    Toothpick.inject(this, scope)

    super.setup()
    lyricsRecycler.setHasFixedSize(true)
    lyricsRecycler.emptyView = emptyView
    val layoutManager = LinearLayoutManager(this)
    lyricsRecycler.layoutManager = layoutManager
    adapter = LyricsAdapter()
    lyricsRecycler.adapter = adapter
  }

  override fun onDestroy() {
    Toothpick.closeScope(this)
    if (isFinishing) {
      Toothpick.closeScope(presenterScope)
    }
    super.onDestroy()
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

  @javax.inject.Scope
  @Target(AnnotationTarget.TYPE)
  @Retention(AnnotationRetention.RUNTIME)
  annotation class Presenter
}
