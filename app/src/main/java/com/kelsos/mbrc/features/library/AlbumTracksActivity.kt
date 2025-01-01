package com.kelsos.mbrc.features.library

import android.graphics.Bitmap
import android.os.Bundle
import android.view.MenuItem
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.activity.OnBackPressedCallback
import androidx.core.os.BundleCompat
import androidx.core.view.isVisible
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.snackbar.Snackbar
import com.kelsos.mbrc.R
import com.kelsos.mbrc.common.ui.EmptyRecyclerView
import com.kelsos.mbrc.common.utilities.RemoteUtils.sha1
import com.kelsos.mbrc.features.queue.PopupActionHandler
import com.raizlabs.android.dbflow.list.FlowCursorList
import com.squareup.picasso.Picasso
import org.koin.android.ext.android.inject
import org.koin.androidx.scope.ScopeActivity
import java.io.File

class AlbumTracksActivity :
  ScopeActivity(),
  AlbumTracksView,
  TrackEntryAdapter.MenuItemSelectedListener {
  private val adapter: TrackEntryAdapter by inject()
  private val actionHandler: PopupActionHandler by inject()
  private val presenter: AlbumTracksPresenter by inject()

  private var album: AlbumInfo? = null
  private lateinit var recyclerView: EmptyRecyclerView

  public override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setContentView(R.layout.activity_album_tracks)
    onBackPressedDispatcher.addCallback(
      this,
      object : OnBackPressedCallback(true) {
        override fun handleOnBackPressed() {
          finish()
        }
      },
    )

    val extras = intent.extras

    if (extras != null) {
      album = BundleCompat.getParcelable(extras, ALBUM, AlbumInfo::class.java)
    }

    val selectedAlbum = album
    if (selectedAlbum == null) {
      finish()
      return
    }

    setSupportActionBar(findViewById(R.id.toolbar))
    val supportActionBar = supportActionBar ?: error("Actionbar should not be null")
    supportActionBar.setDisplayHomeAsUpEnabled(true)
    supportActionBar.setDisplayShowHomeEnabled(true)

    if (selectedAlbum.album.isEmpty()) {
      supportActionBar.setTitle(R.string.non_album_tracks)
    } else {
      supportActionBar.title = selectedAlbum.album
    }

    findViewById<TextView>(R.id.album_tracks__album).text = selectedAlbum.album
    findViewById<TextView>(R.id.album_tracks__artist).text = selectedAlbum.artist
    loadCover(selectedAlbum.artist, selectedAlbum.album)

    presenter.attach(this)
    presenter.load(selectedAlbum)
    adapter.setMenuItemSelectedListener(this)
    recyclerView = findViewById(R.id.list_tracks)
    recyclerView.layoutManager = LinearLayoutManager(baseContext)
    recyclerView.adapter = adapter
    recyclerView.emptyView = findViewById(R.id.empty_view)
    val play = findViewById<Button>(R.id.play_album)
    play.isVisible = true
    play.setOnClickListener {
      presenter.queueAlbum(selectedAlbum.artist, selectedAlbum.album)
    }
  }

  private fun loadCover(
    artist: String,
    album: String,
  ) {
    val image = findViewById<ImageView>(R.id.album_tracks__cover)
    val cache = File(cacheDir, "covers")
    Picasso
      .get()
      .load(File(cache, sha1("${artist}_$album")))
      .noFade()
      .config(Bitmap.Config.RGB_565)
      .error(R.drawable.ic_image_no_cover)
      .placeholder(R.drawable.ic_image_no_cover)
      .resizeDimen(R.dimen.cover_size, R.dimen.cover_size)
      .centerCrop()
      .into(image)
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
    track: Track,
  ) {
    presenter.queue(track, actionHandler.trackSelected(menuItem))
  }

  override fun onItemClicked(track: Track) {
    presenter.queue(track)
  }

  override fun update(cursor: FlowCursorList<Track>) {
    adapter.update(cursor)
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
    const val ALBUM = "albumName"
  }
}
