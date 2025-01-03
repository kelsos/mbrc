package com.kelsos.mbrc.features.library.tracks

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

class BrowseTrackFragment :
  BaseBrowseFragment(),
  MenuItemSelectedListener<Track> {
  private val adapter: TrackEntryAdapter by inject()
  private val viewModel: BrowseTrackViewModel by viewModel()

  override val emptyTitleRes: Int = R.string.common_empty_no_tracks

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
    adapter.setCoverMode(true)

    observeLoadState(adapter.loadStateFlow, adapter)

    lifecycleScope.launch {
      viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
        viewModel.tracks.collect {
          adapter.submitData(it)
        }
      }
    }

    lifecycleScope.launch {
      viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
        viewModel.events.collect { event ->
          when (event) {
            is TrackUiMessage.QueueFailed -> queue(false, 0)
            is TrackUiMessage.QueueSuccess -> queue(true, event.tracksCount)
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
    item: Track,
    id: Int?,
  ) {
    viewModel.queue(if (id != null) determineTrackQueueAction(id) else Queue.Default, item)
  }
}
