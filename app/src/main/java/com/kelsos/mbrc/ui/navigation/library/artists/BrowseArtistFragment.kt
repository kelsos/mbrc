package com.kelsos.mbrc.ui.navigation.library.artists

import android.os.Bundle
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.core.view.isGone
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.snackbar.Snackbar
import com.kelsos.mbrc.R
import com.kelsos.mbrc.adapters.ArtistEntryAdapter
import com.kelsos.mbrc.adapters.ArtistEntryAdapter.MenuItemSelectedListener
import com.kelsos.mbrc.annotations.Queue
import com.kelsos.mbrc.data.library.Artist
import com.kelsos.mbrc.helper.PopupActionHandler
import com.kelsos.mbrc.ui.navigation.library.LibraryActivity.Companion.LIBRARY_SCOPE
import com.kelsos.mbrc.ui.widgets.EmptyRecyclerView
import com.raizlabs.android.dbflow.list.FlowCursorList
import toothpick.Scope
import toothpick.Toothpick
import javax.inject.Inject

class BrowseArtistFragment :
  Fragment(),
  BrowseArtistView,
  MenuItemSelectedListener {
  private lateinit var recycler: EmptyRecyclerView
  private lateinit var emptyView: View
  private lateinit var emptyTitle: TextView

  @Inject
  lateinit var adapter: ArtistEntryAdapter

  @Inject
  lateinit var actionHandler: PopupActionHandler

  @Inject
  lateinit var presenter: BrowseArtistPresenter

  private var scope: Scope? = null
  private lateinit var syncButton: Button

  override fun search(term: String) {
    syncButton.isGone = term.isNotEmpty()
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
      .make(recycler, R.string.queue_result__success, Snackbar.LENGTH_SHORT)
      .setText(message)
      .show()
  }

  override fun onCreate(savedInstanceState: Bundle?) {
    scope = Toothpick.openScopes(requireActivity().application, LIBRARY_SCOPE, activity, this)
    scope?.installModules(BrowseArtistModule())
    super.onCreate(savedInstanceState)
    Toothpick.inject(this, scope)
  }

  override fun onStart() {
    super.onStart()
    presenter.attach(this)
    adapter.refresh()
  }

  override fun onStop() {
    super.onStop()
    presenter.detach()
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

    emptyTitle.setText(R.string.artists_list_empty)
    syncButton = view.findViewById<Button>(R.id.list_empty_sync)
    syncButton.setOnClickListener {
      presenter.sync()
    }
    return view
  }

  override fun onViewCreated(
    view: View,
    savedInstanceState: Bundle?,
  ) {
    super.onViewCreated(view, savedInstanceState)
    recycler.setHasFixedSize(true)
    recycler.adapter = adapter
    recycler.emptyView = emptyView
    recycler.layoutManager = LinearLayoutManager(recycler.context)
    adapter.setMenuItemSelectedListener(this)
    presenter.attach(this)
    presenter.load()
  }

  override fun onMenuItemSelected(
    menuItem: MenuItem,
    artist: Artist,
  ) {
    val action = actionHandler.artistSelected(menuItem, artist, requireActivity())
    if (action != Queue.PROFILE) {
      presenter.queue(action, artist)
    }
  }

  override fun onItemClicked(artist: Artist) {
    actionHandler.artistSelected(artist, requireActivity())
  }

  override fun update(data: FlowCursorList<Artist>) {
    adapter.update(data)
  }
}
