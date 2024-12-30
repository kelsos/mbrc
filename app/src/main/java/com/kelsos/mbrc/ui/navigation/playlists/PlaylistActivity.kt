package com.kelsos.mbrc.ui.navigation.playlists

import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout.OnRefreshListener
import com.google.android.material.snackbar.Snackbar
import com.kelsos.mbrc.R
import com.kelsos.mbrc.adapters.PlaylistAdapter
import com.kelsos.mbrc.adapters.PlaylistAdapter.OnPlaylistPressedListener
import com.kelsos.mbrc.data.Playlist
import com.kelsos.mbrc.ui.activities.BaseActivity
import com.kelsos.mbrc.ui.widgets.EmptyRecyclerView
import com.kelsos.mbrc.ui.widgets.MultiSwipeRefreshLayout
import com.raizlabs.android.dbflow.list.FlowCursorList
import toothpick.Scope
import toothpick.Toothpick
import toothpick.smoothie.module.SmoothieActivityModule
import java.net.ConnectException
import javax.inject.Inject

class PlaylistActivity :
  BaseActivity(),
  PlaylistView,
  OnPlaylistPressedListener,
  OnRefreshListener {
  private val PRESENTER_SCOPE: Class<*> = Presenter::class.java

  private lateinit var swipeLayout: MultiSwipeRefreshLayout
  private lateinit var playlistList: EmptyRecyclerView
  private lateinit var emptyView: View
  private lateinit var emptyViewTitle: TextView

  @Inject
  lateinit var adapter: PlaylistAdapter

  @Inject
  lateinit var presenter: PlaylistPresenter
  private lateinit var scope: Scope

  public override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setContentView(R.layout.activity_playlists)
    swipeLayout = findViewById(R.id.swipe_layout)
    playlistList = findViewById(R.id.playlist_list)
    emptyView = findViewById(R.id.empty_view)
    emptyViewTitle = findViewById(R.id.list_empty_title)

    scope = Toothpick.openScopes(application, PRESENTER_SCOPE, this)
    scope.installTestModules(SmoothieActivityModule(this), PlaylistModule())
    Toothpick.inject(this, scope)

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

  override fun onDestroy() {
    Toothpick.closeScope(this)

    if (isFinishing) {
      Toothpick.closeScope(PRESENTER_SCOPE)
    }
    super.onDestroy()
  }

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

  @javax.inject.Scope
  @Target(AnnotationTarget.TYPE)
  @Retention(AnnotationRetention.RUNTIME)
  annotation class Presenter
}
