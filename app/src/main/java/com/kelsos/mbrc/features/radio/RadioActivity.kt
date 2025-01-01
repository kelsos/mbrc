package com.kelsos.mbrc.features.radio

import android.os.Bundle
import android.view.View
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.google.android.material.snackbar.Snackbar
import com.kelsos.mbrc.BaseActivity
import com.kelsos.mbrc.R
import com.kelsos.mbrc.common.ui.EmptyRecyclerView
import com.kelsos.mbrc.common.ui.MultiSwipeRefreshLayout
import com.raizlabs.android.dbflow.list.FlowCursorList
import org.koin.android.ext.android.inject

class RadioActivity :
  BaseActivity(),
  RadioView,
  SwipeRefreshLayout.OnRefreshListener,
  RadioAdapter.OnRadioPressedListener {
  private lateinit var swipeLayout: MultiSwipeRefreshLayout
  private lateinit var radioView: EmptyRecyclerView
  private lateinit var emptyView: View
  private lateinit var emptyViewTitle: TextView
  private lateinit var emptyViewIcon: ImageView
  private lateinit var emptyViewSubTitle: TextView
  private lateinit var emptyViewProgress: ProgressBar

  private val presenter: RadioPresenter by inject()
  private val adapter: RadioAdapter by inject()

  override fun active(): Int = R.id.nav_radio

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setContentView(R.layout.activity_radio)

    swipeLayout = findViewById(R.id.swipe_layout)
    radioView = findViewById(R.id.radio_list)
    emptyView = findViewById(R.id.empty_view)
    emptyViewTitle = findViewById(R.id.list_empty_title)
    emptyViewIcon = findViewById(R.id.list_empty_icon)
    emptyViewSubTitle = findViewById(R.id.list_empty_subtitle)
    emptyViewProgress = findViewById(R.id.empty_view_progress_bar)

    super.setup()
    swipeLayout.setOnRefreshListener(this)
    swipeLayout.setSwipeableChildren(R.id.radio_list, R.id.empty_view)
    emptyViewTitle.setText(R.string.radio__no_radio_stations)
    emptyViewIcon.setImageResource(R.drawable.ic_radio_black_80dp)
    radioView.adapter = adapter
    radioView.emptyView = emptyView
    radioView.layoutManager = LinearLayoutManager(this)
  }

  override fun onStart() {
    super.onStart()
    presenter.attach(this)
    presenter.load()
    adapter.setOnRadioPressedListener(this)
  }

  override fun onStop() {
    super.onStop()
    presenter.detach()
    adapter.setOnRadioPressedListener(null)
  }

  override fun update(data: FlowCursorList<RadioStation>) {
    adapter.update(data)
  }

  override fun error(error: Throwable) {
    Snackbar.make(radioView, R.string.radio__loading_failed, Snackbar.LENGTH_SHORT).show()
  }

  override fun onRadioPressed(path: String) {
    presenter.play(path)
  }

  override fun onRefresh() {
    presenter.refresh()
  }

  override fun radioPlayFailed() {
    Snackbar.make(radioView, R.string.radio__play_failed, Snackbar.LENGTH_SHORT).show()
  }

  override fun radioPlaySuccessful() {
    Snackbar.make(radioView, R.string.radio__play_successful, Snackbar.LENGTH_SHORT).show()
  }

  override fun showLoading() {
    emptyViewProgress.visibility = View.VISIBLE
    emptyViewIcon.visibility = View.GONE
    emptyViewTitle.visibility = View.GONE
    emptyViewSubTitle.visibility = View.GONE
  }

  override fun hideLoading() {
    emptyViewProgress.visibility = View.GONE
    emptyViewIcon.visibility = View.VISIBLE
    emptyViewTitle.visibility = View.VISIBLE
    emptyViewSubTitle.visibility = View.VISIBLE
    swipeLayout.isRefreshing = false
  }
}
