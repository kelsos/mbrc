package com.kelsos.mbrc.features.library.artists

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
import org.koin.androidx.viewmodel.ext.android.viewModel

class GenreArtistsActivity :
  BaseDetailsActivity(R.layout.activity_genre_artists),
  MenuItemSelectedListener<Artist> {
  private val adapter: ArtistEntryAdapter by inject()
  private val viewModel: GenreArtistsViewModel by viewModel()

  private val id: Long by extraId(GENRE_ID)
  private val genre: String by extraString(GENRE_NAME)

  public override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)

    if (id <= 0) {
      finish()
      return
    }

    setToolbarTitle(genre)
    setAdapter(adapter)
    adapter.setMenuItemSelectedListener(this)

    lifecycleScope.launch {
      adapter.loadStateFlow.map { it.refresh }.distinctUntilChanged().collectLatest { loadState ->
        setEmptyState(loadState is LoadState.NotLoading && adapter.itemCount == 0)
      }
    }

    lifecycleScope.launch {
      repeatOnLifecycle(Lifecycle.State.STARTED) {
        viewModel.artists.collect {
          adapter.submitData(it)
        }
      }
    }

    lifecycleScope.launch {
      repeatOnLifecycle(Lifecycle.State.STARTED) {
        viewModel.events.collect { event ->
          when (event) {
            is ArtistUiMessage.OpenArtistAlbums -> openArtistDetails(event.artist)
            is ArtistUiMessage.QueueFailed -> queue(false, 0)
            is ArtistUiMessage.QueueSuccess -> queue(true, event.tracksCount)
            ArtistUiMessage.NetworkUnavailable -> networkUnavailable()
          }
        }
      }
    }

    viewModel.load(id)
  }

  override fun onAction(
    item: Artist,
    id: Int?,
  ) {
    val action = if (id !== null) determineArtistQueueAction(id) else Queue.Default
    viewModel.queue(action, item)
  }

  companion object {
    const val GENRE_ID = "genre_id"
    const val GENRE_NAME = "genre_name"
  }
}
