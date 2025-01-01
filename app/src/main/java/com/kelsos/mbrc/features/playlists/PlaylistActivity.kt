package com.kelsos.mbrc.features.playlists

import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.google.android.material.snackbar.Snackbar
import com.kelsos.mbrc.BaseActivity
import com.kelsos.mbrc.R
import com.kelsos.mbrc.common.ui.EmptyRecyclerView
import com.kelsos.mbrc.common.ui.MultiSwipeRefreshLayout
import com.raizlabs.android.dbflow.list.FlowCursorList
import org.koin.android.ext.android.inject
import java.net.ConnectException

class PlaylistActivity :
  BaseActivity(),
  PlaylistView,
  PlaylistAdapter.OnPlaylistPressedListener,
  SwipeRefreshLayout.OnRefreshListener {
  private lateinit var swipeLayout: MultiSwipeRefreshLayout
  private lateinit var playlistList: EmptyRecyclerView
  private lateinit var emptyView: View
  private lateinit var emptyViewTitle: TextView

  private val adapter: PlaylistAdapter by inject()
  private val presenter: PlaylistPresenter by inject()

  public override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setContentView(R.layout.activity_playlists)
    swipeLayout = findViewById(R.id.swipe_layout)
    playlistList = findViewById(R.id.playlist_list)
    emptyView = findViewById(R.id.empty_view)
    emptyViewTitle = findViewById(R.id.list_empty_title)

    super.setup()

    swipeLayout.setSwipeableChildren(R.id.playlist_list, R.id.empty_view)
    adapter.setPlaylistPressedListener(this)
    playlistList.layoutManager = LinearLayoutManager(this)
    playlistList.emptyView = emptyView
    playlistList.adapter = adapter
    swipeLayout.setOnRefreshListener(this)
    emptyViewTitle.setText(R.string.playlists_list_empty)
  }

  public override fun onStart() {
    super.onStart()
    presenter.attach(this)
    presenter.load()
  }

  public override fun onStop() {
    super.onStop()
    presenter.detach()
  }

  override fun playlistPressed(path: String) {
    presenter.play(path)
  }

  override fun active(): Int = R.id.nav_playlists

  override fun onRefresh() {
    if (!swipeLayout.isRefreshing) {
      swipeLayout.isRefreshing = true
    }

    presenter.reload()
  }

  override fun update(cursor: FlowCursorList<Playlist>) {
    adapter.update(cursor)
    swipeLayout.isRefreshing = false
  }

  override fun failure(throwable: Throwable) {
    swipeLayout.isRefreshing = false
    if (throwable.cause is ConnectException) {
      Snackbar.make(swipeLayout, R.string.service_connection_error, Snackbar.LENGTH_SHORT).show()
    } else {
      Snackbar.make(swipeLayout, R.string.playlists_load_failed, Snackbar.LENGTH_SHORT).show()
    }
  }
}
