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
import com.kelsos.mbrc.extensions.enableHome
import com.kelsos.mbrc.features.library.ArtistEntryAdapter.MenuItemSelectedListener
import com.kelsos.mbrc.features.queue.PopupActionHandler
import com.raizlabs.android.dbflow.list.FlowCursorList
import org.koin.android.ext.android.inject
import org.koin.androidx.scope.ScopeActivity

class GenreArtistsActivity :
  ScopeActivity(),
  GenreArtistsView,
  MenuItemSelectedListener {
  private lateinit var recyclerView: EmptyRecyclerView
  private lateinit var toolbar: MaterialToolbar
  private lateinit var emptyView: ConstraintLayout

  private val adapter: ArtistEntryAdapter by inject()
  private val actionHandler: PopupActionHandler by inject()
  private val presenter: GenreArtistsPresenter by inject()

  private var genre: String? = null

  public override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setContentView(R.layout.activity_genre_artists)

    onBackPressedDispatcher.addCallback(
      this,
      object : OnBackPressedCallback(true) {
        override fun handleOnBackPressed() {
          finish()
        }
      },
    )

    recyclerView = findViewById(R.id.genre_artists_recycler)
    toolbar = findViewById(R.id.toolbar)
    emptyView = findViewById(R.id.empty_view)

    genre = intent?.extras?.getString(GENRE_NAME)

    if (genre == null) {
      finish()
      return
    }

    setSupportActionBar(toolbar)
    supportActionBar?.enableHome(genre)
    if (genre.isNullOrEmpty()) {
      supportActionBar?.setTitle(R.string.empty)
    }
    adapter.setMenuItemSelectedListener(this)
    recyclerView.adapter = adapter
    recyclerView.emptyView = emptyView
    recyclerView.layoutManager = LinearLayoutManager(recyclerView.context)
    presenter.attach(this)
    presenter.load(genre!!)
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
    artist: Artist,
  ) {
    val action = actionHandler.artistSelected(menuItem, artist, this)
    if (action != Queue.PROFILE) {
      presenter.queue(action, artist)
    }
  }

  override fun onItemClicked(artist: Artist) {
    actionHandler.artistSelected(artist, this)
  }

  override fun update(data: FlowCursorList<Artist>) {
    adapter.update(data)
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
    const val GENRE_NAME = "genre_name"
  }
}
