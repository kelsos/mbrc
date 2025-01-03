package com.kelsos.mbrc.features.lyrics

import android.os.Bundle
import android.widget.TextView
import androidx.constraintlayout.widget.Group
import androidx.core.view.isGone
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import com.kelsos.mbrc.BaseActivity
import com.kelsos.mbrc.R
import com.kelsos.mbrc.common.ui.EmptyRecyclerView
import kotlinx.coroutines.launch
import org.koin.androidx.viewmodel.ext.android.viewModel

class LyricsActivity : BaseActivity(R.layout.activity_lyrics) {
  private lateinit var lyricsRecycler: EmptyRecyclerView
  private lateinit var emptyView: Group
  private lateinit var emptyText: TextView

  override fun active(): Int = R.id.nav_lyrics

  private val viewModel: LyricsViewModel by viewModel()
  private lateinit var adapter: LyricsAdapter

  public override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    lyricsRecycler = findViewById(R.id.lyrics_recycler_view)
    emptyView = findViewById(R.id.empty_view)
    emptyText = findViewById(R.id.empty_view_text)

    val layoutManager = LinearLayoutManager(this)
    adapter = LyricsAdapter()
    lyricsRecycler.setHasFixedSize(true)
    lyricsRecycler.emptyView = emptyView
    lyricsRecycler.layoutManager = layoutManager
    lyricsRecycler.adapter = adapter

    lifecycleScope.launch {
      repeatOnLifecycle(Lifecycle.State.STARTED) {
        viewModel.lyrics.collect {
          emptyView.isGone = it.isNotEmpty()
          adapter.submitList(it)

          if (it.isEmpty()) {
            emptyText.setText(R.string.no_lyrics)
          }
        }
      }
    }
  }
}
