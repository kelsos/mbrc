package com.kelsos.mbrc.features.library

import android.os.Bundle
import android.view.MenuItem
import androidx.activity.OnBackPressedCallback
import androidx.annotation.LayoutRes
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.isVisible
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.snackbar.Snackbar
import com.kelsos.mbrc.R
import com.kelsos.mbrc.common.ui.EmptyRecyclerView
import org.koin.androidx.scope.ScopeActivity

interface MenuItemSelectedListener<T : Any> {
  fun onAction(
    item: T,
    id: Int? = null,
  )
}

open class BaseDetailsActivity(
  @LayoutRes contentLayoutId: Int,
) : ScopeActivity(contentLayoutId) {
  private lateinit var recycler: EmptyRecyclerView
  private lateinit var toolbar: MaterialToolbar
  private lateinit var emptyView: ConstraintLayout

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)

    recycler = findViewById(R.id.details_recycler)
    toolbar = findViewById(R.id.toolbar)
    emptyView = findViewById(R.id.empty_view)

    addBackPressedHandler()
    setSupportActionBar(toolbar)
    supportActionBar?.setDisplayHomeAsUpEnabled(true)
    supportActionBar?.setDisplayShowHomeEnabled(true)

    recycler.emptyView = emptyView
    recycler.layoutManager = LinearLayoutManager(recycler.context)
  }

  protected fun setAdapter(adapter: RecyclerView.Adapter<*>) {
    recycler.adapter = adapter
  }

  protected fun setEmptyState(isEmpty: Boolean) {
    recycler.isVisible = !isEmpty
    emptyView.isVisible = isEmpty
  }

  protected fun setToolbarTitle(title: String) {
    supportActionBar?.title = title.ifEmpty { getString(R.string.empty) }
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

  private fun addBackPressedHandler() {
    onBackPressedDispatcher.addCallback(
      this,
      object : OnBackPressedCallback(true) {
        override fun handleOnBackPressed() {
          finish()
        }
      },
    )
  }

  override fun onOptionsItemSelected(item: MenuItem): Boolean {
    val itemId = item.itemId

    if (itemId == android.R.id.home) {
      onBackPressedDispatcher.onBackPressed()
      return true
    }

    return super.onOptionsItemSelected(item)
  }
}
