package com.kelsos.mbrc.features.library.artists

import android.os.Bundle
import android.view.View
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.kelsos.mbrc.R
import com.kelsos.mbrc.features.library.BaseBrowseFragment
import com.kelsos.mbrc.features.library.MenuItemSelectedListener
import com.kelsos.mbrc.features.queue.Queue
import kotlinx.coroutines.launch
import org.koin.android.ext.android.inject
import org.koin.androidx.viewmodel.ext.android.viewModel

class BrowseArtistFragment :
  BaseBrowseFragment(),
  MenuItemSelectedListener<Artist> {
  private val adapter: ArtistEntryAdapter by inject()
  private val viewModel: BrowseArtistViewModel by viewModel()

  override val emptyTitleRes: Int = R.string.artists_list_empty

  override fun onSyncPressed() {
    viewModel.sync()
  }

  override fun onViewCreated(
    view: View,
    savedInstanceState: Bundle?,
  ) {
    super.onViewCreated(view, savedInstanceState)
    setAdapter(adapter)
    adapter.setMenuItemSelectedListener(this)

    observeLoadState(adapter.loadStateFlow, adapter)

    lifecycleScope.launch {
      viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
        viewModel.artists.collect {
          adapter.submitData(it)
        }
      }
    }

    lifecycleScope.launch {
      viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
        viewModel.events.collect { event ->
          when (event) {
            is ArtistUiMessage.QueueFailed -> queue(false, 0)
            is ArtistUiMessage.QueueSuccess -> queue(true, event.tracksCount)
            is ArtistUiMessage.OpenArtistAlbums -> requireContext().openArtistDetails(event.artist)
            ArtistUiMessage.NetworkUnavailable -> networkUnavailable()
          }
        }
      }
    }

    lifecycleScope.launch {
      viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
        viewModel.showSync.collect { showSync ->
          setSyncButtonVisibility(showSync)
        }
      }
    }
  }

  override fun onAction(
    item: Artist,
    id: Int?,
  ) {
    viewModel.queue(if (id != null) determineArtistQueueAction(id) else Queue.Default, item)
  }
}
