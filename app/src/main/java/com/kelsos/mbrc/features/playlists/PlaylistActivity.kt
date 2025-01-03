package com.kelsos.mbrc.features.playlists

import android.os.Bundle
import android.view.View
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import androidx.core.view.isGone
import androidx.core.view.isVisible
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.paging.LoadState
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.google.android.material.snackbar.Snackbar
import com.kelsos.mbrc.BaseActivity
import com.kelsos.mbrc.R
import com.kelsos.mbrc.common.ui.EmptyRecyclerView
import com.kelsos.mbrc.common.ui.MultiSwipeRefreshLayout
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import org.koin.android.ext.android.inject
import org.koin.androidx.viewmodel.ext.android.viewModel

class PlaylistActivity :
  BaseActivity(R.layout.activity_playlists),
  PlaylistAdapter.OnPlaylistPressedListener,
  SwipeRefreshLayout.OnRefreshListener {
  private lateinit var swipeLayout: MultiSwipeRefreshLayout
  private lateinit var playlistList: EmptyRecyclerView
  private lateinit var emptyView: View
  private lateinit var emptyViewTitle: TextView
  private lateinit var emptyViewIcon: ImageView
  private lateinit var emptyViewSubTitle: TextView
  private lateinit var emptyViewProgress: ProgressBar

  private val adapter: PlaylistAdapter by inject()
  private val viewModel: PlaylistViewModel by viewModel()

  override fun active(): Int = R.id.nav_playlists

  public override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)

    initViews()

    swipeLayout.setSwipeableChildren(R.id.playlist_list, R.id.empty_view)
    emptyViewTitle.setText(R.string.playlists_list_empty)
    playlistList.adapter = adapter
    playlistList.emptyView = emptyView
    playlistList.layoutManager = LinearLayoutManager(this)
    swipeLayout.setOnRefreshListener(this)

    observeViewModel()

    viewModel.actions.reload()
  }

  private fun observeViewModel() {
    lifecycleScope.launch {
      adapter.loadStateFlow.map { it.refresh }.distinctUntilChanged().collectLatest { loadState ->
        updateEmptyViewState(loadState is LoadState.Loading)
        emptyView.isGone = adapter.itemCount > 0
      }
    }

    lifecycleScope.launch {
      repeatOnLifecycle(Lifecycle.State.STARTED) {
        viewModel.playlists.collect {
          adapter.submitData(it)
        }
      }
    }

    lifecycleScope.launch {
      repeatOnLifecycle(Lifecycle.State.STARTED) {
        viewModel.events.collect { events ->
          processEvents(events)
        }
      }
    }
  }

  private fun processEvents(events: PlaylistUiMessages) {
    swipeLayout.isRefreshing = false
    val resId =
      when (events) {
        PlaylistUiMessages.RefreshFailed -> R.string.playlists_load_failed
        PlaylistUiMessages.RefreshSuccess -> R.string.playlists_load_success
      }

    Snackbar.make(swipeLayout, resId, Snackbar.LENGTH_SHORT).show()
  }

  private fun initViews() {
    swipeLayout = findViewById(R.id.swipe_layout)
    playlistList = findViewById(R.id.playlist_list)
    emptyView = findViewById(R.id.empty_view)
    emptyViewTitle = findViewById(R.id.list_empty_title)
    emptyViewIcon = findViewById(R.id.list_empty_icon)
    emptyViewSubTitle = findViewById(R.id.list_empty_subtitle)
    emptyViewProgress = findViewById(R.id.empty_view_progress_bar)
  }

  override fun onStart() {
    super.onStart()
    adapter.setPlaylistPressedListener(this)
  }

  override fun onStop() {
    super.onStop()
    adapter.setPlaylistPressedListener(null)
  }

  override fun playlistPressed(path: String) {
    viewModel.actions.play(path)
  }

  override fun onRefresh() {
    if (!swipeLayout.isRefreshing) {
      swipeLayout.isRefreshing = true
    }

    viewModel.actions.reload()
  }

  fun updateEmptyViewState(showLoading: Boolean) {
    emptyViewProgress.isVisible = showLoading
    emptyViewIcon.isGone = showLoading
    emptyViewTitle.isGone = showLoading
    emptyViewSubTitle.isGone = showLoading
    emptyViewIcon.isGone = showLoading

    if (!showLoading) {
      swipeLayout.isRefreshing = false
    }
  }
}
