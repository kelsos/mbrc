package com.kelsos.mbrc.ui.navigation.playlists

import android.os.Bundle
import android.support.design.widget.Snackbar
import android.support.v4.widget.SwipeRefreshLayout.OnRefreshListener
import android.support.v7.widget.LinearLayoutManager
import android.view.View
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import butterknife.BindView
import butterknife.ButterKnife
import com.kelsos.mbrc.R
import com.kelsos.mbrc.content.playlists.Playlist
import com.kelsos.mbrc.content.playlists.PlaylistService
import com.kelsos.mbrc.ui.activities.BaseActivity
import com.kelsos.mbrc.ui.navigation.playlists.PlaylistAdapter.OnPlaylistPressedListener
import com.kelsos.mbrc.ui.widgets.EmptyRecyclerView
import com.kelsos.mbrc.ui.widgets.MultiSwipeRefreshLayout
import com.raizlabs.android.dbflow.list.FlowCursorList
import toothpick.Scope
import toothpick.Toothpick
import toothpick.smoothie.module.SmoothieActivityModule
import java.net.ConnectException
import javax.inject.Inject

class PlaylistActivity : BaseActivity(),
                         PlaylistView,
                         OnPlaylistPressedListener,
                         OnRefreshListener {

  private val PRESENTER_SCOPE: Class<*> = Presenter::class.java

  @BindView(R.id.swipe_layout) lateinit var swipeLayout: MultiSwipeRefreshLayout
  @BindView(R.id.playlist_list) lateinit var playlistList: EmptyRecyclerView
  @BindView(R.id.empty_view) lateinit var emptyView: View
  @BindView(R.id.list_empty_title) lateinit var emptyViewTitle: TextView
  @BindView(R.id.list_empty_icon) lateinit var emptyViewIcon: ImageView
  @BindView(R.id.list_empty_subtitle) lateinit var emptyViewSubTitle: TextView
  @BindView(R.id.empty_view_progress_bar) lateinit var emptyViewProgress: ProgressBar

  @Inject lateinit var adapter: PlaylistAdapter
  @Inject lateinit var service: PlaylistService
  @Inject lateinit var presenter: PlaylistPresenter
  private lateinit var scope: Scope

  public override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setContentView(R.layout.activity_playlists)
    ButterKnife.bind(this)

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
    presenter.load()
  }

  public override fun onStart() {
    super.onStart()
    presenter.attach(this)
  }

  public override fun onStop() {
    super.onStop()
    presenter.detach()
  }

  override fun playlistPressed(path: String) {
    presenter.play(path)
  }

  override fun active(): Int {
    return R.id.nav_playlists
  }

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

  override fun showLoading() {
    emptyViewProgress.visibility = View.VISIBLE
    emptyViewIcon.visibility = View.GONE
    emptyViewTitle.visibility = View.GONE
    emptyViewSubTitle.visibility = View.GONE
  }

  override fun hideLoading() {
    emptyViewProgress.visibility = View.GONE
    emptyViewIcon.visibility = View.VISIBLE
    emptyViewTitle.visibility = View.VISIBLE
    emptyViewSubTitle.visibility = View.VISIBLE
    swipeLayout.isRefreshing = false
  }

  @javax.inject.Scope
  @Target(AnnotationTarget.TYPE)
  @Retention(AnnotationRetention.RUNTIME)
  annotation class Presenter
}
