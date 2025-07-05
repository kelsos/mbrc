package com.kelsos.mbrc.features.library.albums

import android.os.Bundle
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.paging.LoadState
import com.kelsos.mbrc.R
import com.kelsos.mbrc.features.library.BaseDetailsActivity
import com.kelsos.mbrc.features.library.MenuItemSelectedListener
import com.kelsos.mbrc.features.library.extraId
import com.kelsos.mbrc.features.library.extraString
import com.kelsos.mbrc.features.queue.Queue
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import org.koin.android.ext.android.inject

class ArtistAlbumsActivity :
  BaseDetailsActivity(R.layout.activity_artist_albums),
  MenuItemSelectedListener<Album> {
  private val adapter: AlbumEntryAdapter by inject()
  private val viewModel: ArtistAlbumsViewModel by inject()

  private val id: Long by extraId(ARTIST_ID)
  private val artist: String by extraString(ARTIST_NAME)

  public override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)

    if (id <= 0) {
      finish()
      return
    }

    setToolbarTitle(artist)
    setAdapter(adapter)
    adapter.setMenuItemSelectedListener(this)

    lifecycleScope.launch {
      adapter.loadStateFlow.map { it.refresh }.distinctUntilChanged().collectLatest { loadState ->
        setEmptyState(loadState is LoadState.NotLoading && adapter.itemCount == 0)
      }
    }

    lifecycleScope.launch {
      repeatOnLifecycle(Lifecycle.State.STARTED) {
        viewModel.albums.collect {
          adapter.submitData(it)
        }
      }
    }

    lifecycleScope.launch {
      repeatOnLifecycle(Lifecycle.State.STARTED) {
        viewModel.events.collect { event ->
          when (event) {
            is AlbumUiMessage.OpenAlbumTracks -> openAlbumDetails(event.album)
            AlbumUiMessage.QueueFailed -> queue(false, 0)
            is AlbumUiMessage.QueueSuccess -> queue(true, event.tracksCount)
            AlbumUiMessage.NetworkUnavailable -> networkUnavailable()
          }
        }
      }
    }

    viewModel.load(artist)
  }

  override fun onAction(
    item: Album,
    id: Int?,
  ) {
    val action = if (id != null) determineAlbumQueueAction(id) else Queue.Default
    viewModel.queue(action, item)
  }

  companion object {
    const val ARTIST_NAME = "artist_name"
    const val ARTIST_ID = "artist_id"
  }
}
