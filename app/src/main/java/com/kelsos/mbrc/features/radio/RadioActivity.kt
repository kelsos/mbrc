package com.kelsos.mbrc.features.radio

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

class RadioActivity :
  BaseActivity(R.layout.activity_radio),
  SwipeRefreshLayout.OnRefreshListener,
  RadioAdapter.OnRadioPressedListener {
  private lateinit var swipeLayout: MultiSwipeRefreshLayout
  private lateinit var radioView: EmptyRecyclerView
  private lateinit var emptyView: View
  private lateinit var emptyViewTitle: TextView
  private lateinit var emptyViewIcon: ImageView
  private lateinit var emptyViewSubTitle: TextView
  private lateinit var emptyViewProgress: ProgressBar

  private val adapter: RadioAdapter by inject()
  private val viewModel: RadioViewModel by viewModel()

  override fun active(): Int = R.id.nav_radio

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)

    swipeLayout = findViewById(R.id.swipe_layout)
    radioView = findViewById(R.id.radio_list)
    emptyView = findViewById(R.id.empty_view)
    emptyViewTitle = findViewById(R.id.list_empty_title)
    emptyViewIcon = findViewById(R.id.list_empty_icon)
    emptyViewSubTitle = findViewById(R.id.list_empty_subtitle)
    emptyViewProgress = findViewById(R.id.empty_view_progress_bar)

    swipeLayout.setOnRefreshListener(this)
    swipeLayout.setSwipeableChildren(R.id.radio_list, R.id.empty_view)
    emptyViewTitle.setText(R.string.radio__no_radio_stations)
    emptyViewIcon.setImageResource(R.drawable.baseline_radio_80)
    radioView.adapter = adapter
    radioView.emptyView = emptyView
    radioView.layoutManager = LinearLayoutManager(this)

    lifecycleScope.launch {
      adapter.loadStateFlow.map { it.refresh }.distinctUntilChanged().collectLatest { loadState ->
        updateEmptyViewState(loadState is LoadState.Loading)
        emptyView.isGone = loadState is LoadState.NotLoading || adapter.itemCount > 0
      }
    }

    lifecycleScope.launch {
      repeatOnLifecycle(Lifecycle.State.STARTED) {
        viewModel.radios.collect {
          adapter.submitData(it)
        }
      }
    }

    lifecycleScope.launch {
      repeatOnLifecycle(Lifecycle.State.STARTED) {
        viewModel.events.collect {
          val resId =
            when (it) {
              RadioUiMessages.QueueFailed -> R.string.radio__play_failed
              RadioUiMessages.QueueSuccess -> R.string.radio__play_successful
              RadioUiMessages.RefreshFailed -> {
                swipeLayout.isRefreshing = false
                R.string.radio__loading_failed
              }
              RadioUiMessages.RefreshSuccess -> {
                swipeLayout.isRefreshing = false
                R.string.radio__loading_success
              }
            }
          Snackbar.make(radioView, resId, Snackbar.LENGTH_SHORT).show()
        }
      }
    }
  }

  override fun onStart() {
    super.onStart()
    adapter.setOnRadioPressedListener(this)
  }

  override fun onStop() {
    super.onStop()
    adapter.setOnRadioPressedListener(null)
  }

  override fun onRadioPressed(path: String) {
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

    if (!showLoading) {
      swipeLayout.isRefreshing = false
    }
  }
}
