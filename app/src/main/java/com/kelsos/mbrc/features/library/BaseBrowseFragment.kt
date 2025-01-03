package com.kelsos.mbrc.features.library

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.annotation.StringRes
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import androidx.paging.CombinedLoadStates
import androidx.paging.LoadState
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.snackbar.Snackbar
import com.kelsos.mbrc.R
import com.kelsos.mbrc.common.ui.EmptyRecyclerView
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import org.koin.androidx.scope.ScopeFragment
import timber.log.Timber

abstract class BaseBrowseFragment : ScopeFragment() {
  private lateinit var recycler: EmptyRecyclerView
  private lateinit var emptyView: View
  private lateinit var emptyTitle: TextView
  private lateinit var syncButton: Button

  @get:StringRes
  abstract val emptyTitleRes: Int

  abstract fun onSyncPressed()

  protected fun setAdapter(adapter: RecyclerView.Adapter<*>) {
    recycler.adapter = adapter
  }

  private fun setEmptyState(state: Boolean) {
    recycler.isVisible = !state
    emptyView.isVisible = state
  }

  protected fun setSyncButtonVisibility(visible: Boolean) {
    syncButton.isVisible = visible
  }

  protected fun observeLoadState(
    loadStateFlow: Flow<CombinedLoadStates>,
    adapter: RecyclerView.Adapter<*>,
  ) {
    lifecycleScope.launch {
      loadStateFlow.map { it.refresh }.distinctUntilChanged().collectLatest { loadState ->
        Timber.d("LoadState: $loadState")
        setEmptyState(loadState is LoadState.NotLoading && adapter.itemCount == 0)
      }
    }
  }

  override fun onCreateView(
    inflater: LayoutInflater,
    container: ViewGroup?,
    savedInstanceState: Bundle?,
  ): View? {
    val view = inflater.inflate(R.layout.fragment_library_search, container, false)
    recycler = view.findViewById(R.id.library_data_list)
    emptyView = view.findViewById(R.id.empty_view)
    emptyTitle = view.findViewById(R.id.list_empty_title)
    emptyTitle.setText(emptyTitleRes)

    recycler.emptyView = emptyView
    recycler.layoutManager = LinearLayoutManager(recycler.context)
    recycler.setHasFixedSize(true)

    syncButton = view.findViewById(R.id.list_empty_sync)
    syncButton.setOnClickListener { onSyncPressed() }
    return view
  }

  protected fun queue(
    success: Boolean,
    tracks: Int,
  ) {
    val message =
      if (success) {
        getString(R.string.queue_result__success, resources.getQuantityString(R.plurals.track, tracks, tracks))
      } else {
        getString(R.string.queue_result__failure)
      }
    Snackbar
      .make(recycler, R.string.queue_result__success_title, Snackbar.LENGTH_SHORT)
      .setText(message)
      .show()
  }
}
