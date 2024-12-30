package com.kelsos.mbrc.ui.navigation.library.gernes

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
import com.kelsos.mbrc.adapters.GenreEntryAdapter
import com.kelsos.mbrc.adapters.GenreEntryAdapter.MenuItemSelectedListener
import com.kelsos.mbrc.annotations.Queue
import com.kelsos.mbrc.data.library.Genre
import com.kelsos.mbrc.helper.PopupActionHandler
import com.kelsos.mbrc.ui.navigation.library.LibraryActivity.Companion.LIBRARY_SCOPE
import com.kelsos.mbrc.ui.widgets.EmptyRecyclerView
import com.raizlabs.android.dbflow.list.FlowCursorList
import toothpick.Toothpick
import javax.inject.Inject

class BrowseGenreFragment :
  Fragment(),
  BrowseGenreView,
  MenuItemSelectedListener {
  private lateinit var recycler: EmptyRecyclerView
  private lateinit var emptyView: View
  private lateinit var emptyTitle: TextView

  @Inject
  lateinit var adapter: GenreEntryAdapter

  @Inject
  lateinit var actionHandler: PopupActionHandler

  @Inject
  lateinit var presenter: BrowseGenrePresenter

  private lateinit var syncButton: Button

  override fun onCreateView(
    inflater: LayoutInflater,
    container: ViewGroup?,
    savedInstanceState: Bundle?,
  ): View? {
    val view = inflater.inflate(R.layout.fragment_library_search, container, false)
    recycler = view.findViewById(R.id.library_data_list)
    emptyView = view.findViewById(R.id.empty_view)
    emptyTitle = view.findViewById(R.id.list_empty_title)

    emptyTitle.setText(R.string.genres_list_empty)
    syncButton = view.findViewById(R.id.list_empty_sync)
    syncButton.setOnClickListener {
      presenter.sync()
    }
    return view
  }

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
    super.onCreate(savedInstanceState)
    val scope = Toothpick.openScopes(requireActivity().application, LIBRARY_SCOPE, activity, this)
    scope.installModules(BrowseGenreModule())
    Toothpick.inject(this, scope)
    presenter.attach(this)
    presenter.load()
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

  override fun update(cursor: FlowCursorList<Genre>) {
    adapter.update(cursor)
  }

  override fun onViewCreated(
    view: View,
    savedInstanceState: Bundle?,
  ) {
    super.onViewCreated(view, savedInstanceState)
    recycler.adapter = adapter
    recycler.emptyView = emptyView
    recycler.layoutManager = LinearLayoutManager(recycler.context)
    recycler.setHasFixedSize(true)
    adapter.setMenuItemSelectedListener(this)
  }

  override fun onMenuItemSelected(
    menuItem: MenuItem,
    genre: Genre,
  ): Boolean {
    val action = actionHandler.genreSelected(menuItem, genre, requireActivity())
    if (action != Queue.PROFILE) {
      presenter.queue(action, genre)
    }
    return true
  }

  override fun onItemClicked(genre: Genre) {
    actionHandler.genreSelected(genre, requireActivity())
  }

  override fun onDestroy() {
    Toothpick.closeScope(this)
    super.onDestroy()
  }
}
