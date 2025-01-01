package com.kelsos.mbrc.features.library

import android.os.Bundle
import android.view.MenuItem
import androidx.activity.OnBackPressedCallback
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.snackbar.Snackbar
import com.kelsos.mbrc.R
import com.kelsos.mbrc.annotations.Queue
import com.kelsos.mbrc.common.ui.EmptyRecyclerView
import com.kelsos.mbrc.features.queue.PopupActionHandler
import com.raizlabs.android.dbflow.list.FlowCursorList
import org.koin.android.ext.android.inject
import org.koin.androidx.scope.ScopeActivity

class ArtistAlbumsActivity :
  ScopeActivity(),
  ArtistAlbumsView,
  AlbumEntryAdapter.MenuItemSelectedListener {
  private lateinit var recyclerView: EmptyRecyclerView
  private lateinit var toolbar: MaterialToolbar
  private lateinit var emptyView: ConstraintLayout

  private val actionHandler: PopupActionHandler by inject()
  private val adapter: AlbumEntryAdapter by inject()
  private val presenter: ArtistAlbumsPresenter by inject()

  private var artist: String? = null

  public override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setContentView(R.layout.activity_artist_albums)

    onBackPressedDispatcher.addCallback(
      this,
      object : OnBackPressedCallback(true) {
        override fun handleOnBackPressed() {
          finish()
        }
      },
    )

    recyclerView = findViewById(R.id.album_recycler)
    toolbar = findViewById(R.id.toolbar)
    emptyView = findViewById(R.id.empty_view)

    val extras = intent.extras
    if (extras != null) {
      artist = extras.getString(ARTIST_NAME)
    }

    if (artist == null) {
      finish()
      return
    }

    setSupportActionBar(toolbar)
    supportActionBar?.setDisplayHomeAsUpEnabled(true)
    supportActionBar?.setDisplayShowHomeEnabled(true)
    supportActionBar?.title = artist

    if (artist.isNullOrEmpty()) {
      supportActionBar?.setTitle(R.string.empty)
    }

    adapter.setMenuItemSelectedListener(this)
    recyclerView.layoutManager =
      LinearLayoutManager(this)
    recyclerView.adapter = adapter
    recyclerView.emptyView = emptyView
    presenter.attach(this)
    presenter.load(artist!!)
  }

  override fun onOptionsItemSelected(item: MenuItem): Boolean {
    val itemId = item.itemId

    if (itemId == android.R.id.home) {
      onBackPressedDispatcher.onBackPressed()
      return true
    }

    return super.onOptionsItemSelected(item)
  }

  override fun onMenuItemSelected(
    menuItem: MenuItem,
    album: Album,
  ) {
    val action = actionHandler.albumSelected(menuItem, album, this)
    if (action != Queue.PROFILE) {
      presenter.queue(action, album)
    }
  }

  override fun onItemClicked(album: Album) {
    actionHandler.albumSelected(album, this)
  }

  override fun update(albums: FlowCursorList<Album>) {
    adapter.update(albums)
  }

  override fun queue(
    success: Boolean,
    tracks: Int,
  ) {
    val message =
      if (success) {
        getString(R.string.queue_result__success, tracks)
      } else {
        getString(R.string.queue_result__failure)
      }
    Snackbar
      .make(recyclerView, R.string.queue_result__success, Snackbar.LENGTH_SHORT)
      .setText(message)
      .show()
  }

  override fun onStart() {
    super.onStart()
    presenter.attach(this)
  }

  override fun onStop() {
    super.onStop()
    presenter.detach()
  }

  companion object {
    const val ARTIST_NAME = "artist_name"
  }
}
