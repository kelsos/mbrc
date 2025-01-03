package com.kelsos.mbrc.features.library.tracks

import android.os.Bundle
import android.widget.ImageView
import android.widget.TextView
import androidx.core.view.isVisible
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.paging.LoadState
import coil3.load
import coil3.request.crossfade
import coil3.request.error
import coil3.request.placeholder
import coil3.size.Scale
import com.kelsos.mbrc.R
import com.kelsos.mbrc.common.ui.SquareImageButton
import com.kelsos.mbrc.features.library.BaseDetailsActivity
import com.kelsos.mbrc.features.library.MenuItemSelectedListener
import com.kelsos.mbrc.features.library.albumInfo
import com.kelsos.mbrc.features.library.albums.AlbumInfo
import com.kelsos.mbrc.features.queue.Queue
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import okio.ByteString.Companion.encodeUtf8
import org.koin.android.ext.android.inject
import org.koin.androidx.viewmodel.ext.android.viewModel
import java.io.File

class AlbumTracksActivity :
  BaseDetailsActivity(R.layout.activity_album_tracks),
  MenuItemSelectedListener<Track> {
  private val adapter: TrackEntryAdapter by inject()
  private val viewModel: AlbumTracksViewModel by viewModel()

  private val album: AlbumInfo? by albumInfo()

  public override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)

    val info = album
    if (info == null) {
      finish()
      return
    }

    setToolbarTitle(info.album.ifEmpty { this.getString(R.string.non_album_tracks) })
    setAdapter(adapter)
    adapter.setMenuItemSelectedListener(this)

    findViewById<TextView>(R.id.album_tracks__album).text = info.album
    findViewById<TextView>(R.id.album_tracks__artist).text = info.artist

    val play = findViewById<SquareImageButton>(R.id.play_album)

    play.isVisible = true
    play.setOnClickListener {
      viewModel.queueAlbum(info)
    }

    loadCover(info.artist, info.album)

    lifecycleScope.launch {
      adapter.loadStateFlow.map { it.refresh }.distinctUntilChanged().collectLatest { loadState ->
        setEmptyState(loadState is LoadState.NotLoading && adapter.itemCount == 0)
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
        viewModel.events.collect { event ->
          when (event) {
            TrackUiMessage.QueueFailed -> queue(false, 0)
            is TrackUiMessage.QueueSuccess -> queue(true, event.tracksCount)
          }
        }
      }
    }

    viewModel.load(info)
  }

  private fun loadCover(
    artist: String,
    album: String,
  ) {
    val image = findViewById<ImageView>(R.id.album_tracks__cover)
    val cache = File(cacheDir, "covers")
    val coverFile = File(cache, "${artist}_$album".encodeUtf8().sha1().hex().uppercase())

    image.load(coverFile) {
      crossfade(false)
      placeholder(R.drawable.ic_image_no_cover)
      error(R.drawable.ic_image_no_cover)
      size(resources.getDimensionPixelSize(R.dimen.list_album_size))
      scale(Scale.FILL)
    }
  }

  override fun onAction(
    item: Track,
    id: Int?,
  ) {
    val action = if (id != null) determineTrackQueueAction(id) else Queue.Default
    viewModel.queue(action, item)
  }

  companion object {
    const val ALBUM = "album"
  }
}
