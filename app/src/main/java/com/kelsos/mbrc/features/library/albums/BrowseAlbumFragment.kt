package com.kelsos.mbrc.features.library.albums

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

class BrowseAlbumFragment :
  BaseBrowseFragment(),
  MenuItemSelectedListener<Album> {
  private val adapter: AlbumEntryAdapter by inject()
  private val viewModel: BrowseAlbumViewModel by viewModel()

  override val emptyTitleRes: Int = R.string.albums_list_empty

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
        viewModel.albums.collect {
          adapter.submitData(it)
        }
      }
    }

    lifecycleScope.launch {
      viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
        viewModel.events.collect { event ->
          when (event) {
            is AlbumUiMessage.OpenAlbumTracks -> requireContext().openAlbumDetails(event.album)
            AlbumUiMessage.QueueFailed -> queue(false, 0)
            is AlbumUiMessage.QueueSuccess -> queue(true, event.tracksCount)
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
    item: Album,
    id: Int?,
  ) {
    viewModel.queue(if (id != null) determineAlbumQueueAction(id) else Queue.Default, item)
  }
}
