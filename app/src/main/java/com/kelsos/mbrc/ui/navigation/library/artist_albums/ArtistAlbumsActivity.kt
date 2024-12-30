package com.kelsos.mbrc.ui.navigation.library.artist_albums

import android.os.Bundle
import android.view.MenuItem
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.snackbar.Snackbar
import com.kelsos.mbrc.R
import com.kelsos.mbrc.adapters.AlbumEntryAdapter
import com.kelsos.mbrc.annotations.Queue
import com.kelsos.mbrc.data.library.Album
import com.kelsos.mbrc.helper.PopupActionHandler
import com.kelsos.mbrc.ui.activities.FontActivity
import com.kelsos.mbrc.ui.widgets.EmptyRecyclerView
import com.raizlabs.android.dbflow.list.FlowCursorList
import toothpick.Scope
import toothpick.Toothpick
import toothpick.smoothie.module.SmoothieActivityModule
import javax.inject.Inject

class ArtistAlbumsActivity :
  FontActivity(),
  ArtistAlbumsView,
  AlbumEntryAdapter.MenuItemSelectedListener {
  private lateinit var recyclerView: EmptyRecyclerView
  private lateinit var toolbar: MaterialToolbar
  private lateinit var emptyView: ConstraintLayout

  @Inject
  lateinit var actionHandler: PopupActionHandler

  @Inject
  lateinit var adapter: AlbumEntryAdapter

  @Inject
  lateinit var presenter: ArtistAlbumsPresenter

  private var artist: String? = null
  private var scope: Scope? = null

  public override fun onCreate(savedInstanceState: Bundle?) {
    scope = Toothpick.openScopes(application, this)
    scope!!.installModules(
      SmoothieActivityModule(this),
      ArtistAlbumsModule(),
    )
    super.onCreate(savedInstanceState)
    Toothpick.inject(this, scope)
    setContentView(R.layout.activity_artist_albums)
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
      onBackPressed()
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

  override fun onDestroy() {
    super.onDestroy()
    Toothpick.closeScope(this)
  }

  override fun onStart() {
    super.onStart()
    presenter.attach(this)
  }

  override fun onStop() {
    super.onStop()
    presenter.detach()
  }

  override fun onBackPressed() {
    finish()
  }

  companion object {
    val ARTIST_NAME = "artist_name"
  }
}
