package com.kelsos.mbrc.features.nowplaying

import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.widget.SearchView
import androidx.appcompat.widget.SearchView.OnQueryTextListener
import androidx.core.view.isGone
import androidx.core.view.isVisible
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.paging.LoadState
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.snackbar.Snackbar
import com.kelsos.mbrc.BaseActivity
import com.kelsos.mbrc.R
import com.kelsos.mbrc.common.state.ConnectionStatus
import com.kelsos.mbrc.common.state.PlayingTrack
import com.kelsos.mbrc.common.ui.EmptyRecyclerView
import com.kelsos.mbrc.common.ui.MultiSwipeRefreshLayout
import com.kelsos.mbrc.features.dragsort.OnStartDragListener
import com.kelsos.mbrc.features.dragsort.SimpleItemTouchHelper
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import org.koin.android.ext.android.inject
import org.koin.androidx.viewmodel.ext.android.viewModel

class NowPlayingActivity :
  BaseActivity(R.layout.activity_nowplaying),
  NowPlayingListener,
  OnQueryTextListener {
  private lateinit var nowPlayingList: EmptyRecyclerView
  private lateinit var swipeLayout: MultiSwipeRefreshLayout
  private lateinit var emptyView: View
  private lateinit var emptyViewTitle: TextView
  private lateinit var emptyViewIcon: ImageView
  private lateinit var emptyViewSubTitle: TextView
  private lateinit var emptyViewProgress: ProgressBar

  private val adapter: NowPlayingAdapter by inject()
  private val viewModel: NowPlayingViewModel by viewModel()

  private var searchView: SearchView? = null
  private var searchMenuItem: MenuItem? = null
  private lateinit var touchListener: NowPlayingTouchListener
  private var itemTouchHelper: ItemTouchHelper? = null

  override fun active(): Int = R.id.nav_now_playing

  private val visibleRangeGetter: VisibleRangeGetter =
    VisibleRangeGetter {
      val layoutManager = nowPlayingList.layoutManager as LinearLayoutManager
      val firstItem = layoutManager.findFirstVisibleItemPosition()
      val lastItem = layoutManager.findLastVisibleItemPosition()
      VisibleRange(firstItem, lastItem)
    }

  private val dragStartListener: OnStartDragListener by lazy {
    val vm = viewModel

    object : OnStartDragListener {
      override fun onStartDrag(viewHolder: RecyclerView.ViewHolder) {
        itemTouchHelper?.startDrag(viewHolder)
        swipeLayout.isRefreshing = false
        swipeLayout.isEnabled = false
        swipeLayout.cancelPendingInputEvents()
      }

      override fun onDragComplete() {
        swipeLayout.isEnabled = true
        vm.actions.move()
      }
    }
  }

  override fun onQueryTextSubmit(query: String): Boolean {
    closeSearch()
    viewModel.actions.search(query)
    return true
  }

  internal fun closeSearch(): Boolean {
    searchView?.let {
      if (it.isShown) {
        it.isIconified = true
        it.isFocusable = false
        it.clearFocus()
        it.setQuery("", false)
        searchMenuItem?.collapseActionView()
        return true
      }
    }
    return false
  }

  override fun onQueryTextChange(newText: String): Boolean = true

  override fun onCreateOptionsMenu(menu: Menu): Boolean {
    menuInflater.inflate(R.menu.nowplaying_search, menu)
    searchMenuItem = menu.findItem(R.id.now_playing_search)
    searchView = searchMenuItem?.actionView as SearchView

    searchView?.queryHint = getString(R.string.now_playing_search_hint)
    searchView?.setIconifiedByDefault(true)
    searchView?.setOnQueryTextListener(this)
    return super.onCreateOptionsMenu(menu)
  }

  public override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)

    onBackPressedDispatcher.addCallback(
      this,
      object : OnBackPressedCallback(true) {
        override fun handleOnBackPressed() {
          if (closeSearch()) {
            return
          }
          // Fix stack overflow by temporarily disabling callback
          isEnabled = false
          onBackPressedDispatcher.onBackPressed()
          isEnabled = true
        }
      },
    )

    initViews()

    swipeLayout.setSwipeableChildren(R.id.now_playing_list, R.id.empty_view)
    nowPlayingList.emptyView = emptyView
    val manager = LinearLayoutManager(this)
    nowPlayingList.layoutManager = manager
    nowPlayingList.adapter = adapter
    nowPlayingList.itemAnimator?.changeDuration = 0
    touchListener =
      NowPlayingTouchListener(this) {
        if (it) {
          swipeLayout.clearSwipeableChildren()
          swipeLayout.isRefreshing = false
          swipeLayout.isEnabled = false
          swipeLayout.cancelPendingInputEvents()
        } else {
          swipeLayout.setSwipeableChildren(R.id.now_playing_list, R.id.empty_view)
          swipeLayout.isEnabled = true
        }
      }
    nowPlayingList.addOnItemTouchListener(touchListener)
    val callback = SimpleItemTouchHelper(adapter)
    itemTouchHelper =
      ItemTouchHelper(callback).also {
        it.attachToRecyclerView(nowPlayingList)
      }
    adapter.setNowPlayingListener(this)
    adapter.setVisibleRangeGetter(visibleRangeGetter)
    adapter.setDragStartListener(dragStartListener)
    swipeLayout.setOnRefreshListener { viewModel.actions.reload() }

    observeViewModel()
    viewModel.actions.reload(showUserMessage = false)
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
        viewModel.tracks.collect {
          adapter.submitData(it)
        }
      }
    }

    lifecycleScope.launch {
      repeatOnLifecycle(Lifecycle.State.STARTED) {
        viewModel.events.collect {
          when (it) {
            is NowPlayingUiMessages.RefreshFailed -> {
              swipeLayout.isRefreshing = false
              Snackbar.make(nowPlayingList, R.string.refresh_failed, Snackbar.LENGTH_SHORT).show()
            }

            NowPlayingUiMessages.RefreshSucceeded -> {
              swipeLayout.isRefreshing = false
            }

            NowPlayingUiMessages.NetworkUnavailable -> {
              swipeLayout.isRefreshing = false
              Snackbar.make(nowPlayingList, R.string.connection_error_network_unavailable, Snackbar.LENGTH_SHORT).show()
            }

            NowPlayingUiMessages.PlayFailed -> {
              Snackbar.make(nowPlayingList, R.string.radio__play_failed, Snackbar.LENGTH_SHORT).show()
            }

            NowPlayingUiMessages.RemoveFailed -> {
              Snackbar.make(nowPlayingList, R.string.refresh_failed, Snackbar.LENGTH_SHORT).show()
            }

            NowPlayingUiMessages.MoveFailed -> {
              Snackbar.make(nowPlayingList, R.string.refresh_failed, Snackbar.LENGTH_SHORT).show()
            }
          }
        }
      }
    }

    lifecycleScope.launch {
      repeatOnLifecycle(Lifecycle.State.STARTED) {
        viewModel.playingTrack.collect { track ->
          trackChanged(track, true)
        }
      }
    }

    lifecycleScope.launch {
      repeatOnLifecycle(Lifecycle.State.STARTED) {
        viewModel.connectionState.collect { status ->
          updateNetworkState(status is ConnectionStatus.Connected)
        }
      }
    }
  }

  private fun initViews() {
    nowPlayingList = findViewById(R.id.now_playing_list)
    swipeLayout = findViewById(R.id.swipe_layout)
    emptyView = findViewById(R.id.empty_view)
    emptyViewTitle = findViewById(R.id.list_empty_title)
    emptyViewIcon = findViewById(R.id.list_empty_icon)
    emptyViewSubTitle = findViewById(R.id.list_empty_subtitle)
    emptyViewProgress = findViewById(R.id.empty_view_progress_bar)
  }

  fun updateEmptyViewState(showLoading: Boolean) {
    emptyViewProgress.isVisible = showLoading
    emptyView.isGone = showLoading
    emptyViewTitle.isGone = showLoading
    emptyViewSubTitle.isGone = showLoading
    emptyViewIcon.isGone = showLoading

    if (!showLoading) {
      swipeLayout.isRefreshing = false
    }
  }

  override fun onPress(position: Int) {
    viewModel.actions.play(position)
  }

  override fun onMove(
    from: Int,
    to: Int,
  ) {
    viewModel.actions.moveTrack(from, to)
  }

  override fun onDismiss(position: Int) {
    viewModel.actions.removeTrack(position)
  }

  fun trackChanged(
    playingTrack: PlayingTrack,
    scrollToTrack: Boolean,
  ) {
    adapter.setPlayingTrack(playingTrack.path)
    if (scrollToTrack) {
      nowPlayingList.scrollToPosition(adapter.getPlayingTrackIndex())
    }
  }

  private fun updateNetworkState(isConnected: Boolean) {
    // Update ItemTouchHelper to enable/disable drag and swipe
    if (isConnected) {
      // Re-enable ItemTouchHelper if not already attached
      itemTouchHelper?.attachToRecyclerView(nowPlayingList)
    } else {
      // Disable ItemTouchHelper to prevent drag and swipe
      itemTouchHelper?.attachToRecyclerView(null)
    }
  }
}
