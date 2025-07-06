package com.kelsos.mbrc.features.library.genres

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

class BrowseGenreFragment :
  BaseBrowseFragment(),
  MenuItemSelectedListener<Genre> {
  private val adapter: GenreEntryAdapter by inject()
  private val viewModel: BrowseGenreViewModel by viewModel()

  override val emptyTitleRes: Int = R.string.genres_list_empty

  override fun onSyncPressed() {
    viewModel.sync()
  }

  override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
    super.onViewCreated(view, savedInstanceState)

    setAdapter(adapter)
    adapter.setMenuItemSelectedListener(this)

    observeLoadState(adapter.loadStateFlow, adapter)

    lifecycleScope.launch {
      viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
        viewModel.genres.collect {
          adapter.submitData(it)
        }
      }
    }

    lifecycleScope.launch {
      viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
        viewModel.events.collect { event ->
          when (event) {
            is GenreUiMessage.OpenArtists -> requireContext().openGenreDetails(event.genre)
            is GenreUiMessage.QueueSuccess -> queue(true, event.tracksCount)
            is GenreUiMessage.QueueFailed -> queue(false, 0)
            GenreUiMessage.NetworkUnavailable -> networkUnavailable()
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

  override fun onAction(item: Genre, id: Int?) {
    viewModel.queue(if (id != null) determineGenreQueueAction(id) else Queue.Default, item)
  }
}
