package com.kelsos.mbrc.ui.navigation.radio

import android.arch.paging.PagedList
import android.os.Bundle
import android.support.constraint.Group
import android.support.v4.widget.SwipeRefreshLayout
import android.support.v4.widget.SwipeRefreshLayout.OnRefreshListener
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import androidx.view.isVisible
import com.kelsos.mbrc.R
import com.kelsos.mbrc.content.radios.RadioStationEntity
import com.kelsos.mbrc.ui.activities.BaseNavigationActivity
import com.kelsos.mbrc.ui.navigation.radio.RadioAdapter.OnRadioPressedListener
import kotterknife.bindView
import toothpick.Scope
import toothpick.Toothpick
import toothpick.smoothie.module.SmoothieActivityModule
import javax.inject.Inject

class RadioActivity : BaseNavigationActivity(), RadioView, OnRefreshListener,
  OnRadioPressedListener {

  private val swipeLayout: SwipeRefreshLayout by bindView(R.id.radio_stations__refresh_layout)
  private val radioView: RecyclerView by bindView(R.id.radio_stations__stations_list)
  private val emptyView: Group by bindView(R.id.radio_stations__empty_group)
  private val emptyViewTitle: TextView by bindView(R.id.radio_stations__text_title)
  private val emptyViewIcon: ImageView by bindView(R.id.radio_stations__empty_icon)
  private val emptyViewProgress: ProgressBar by bindView(R.id.radio_stations__loading_bar)

  @Inject
  lateinit var presenter: RadioPresenter
  @Inject
  lateinit var adapter: RadioAdapter

  override fun active(): Int = R.id.nav_radio

  private lateinit var scope: Scope

  override fun onCreate(savedInstanceState: Bundle?) {
    Toothpick.openScope(PRESENTER_SCOPE).installModules(RadioModule())
    scope = Toothpick.openScopes(application, PRESENTER_SCOPE, this)
    scope.installModules(SmoothieActivityModule(this))
    super.onCreate(savedInstanceState)
    setContentView(R.layout.activity_radio)
    Toothpick.inject(this, scope)
    super.setup()
    setupEmptyView()
    setupRecycler()
    presenter.attach(this)
    presenter.load()

  }

  private fun setupRecycler() {
    swipeLayout.setOnRefreshListener(this)
    radioView.adapter = adapter
    radioView.layoutManager = LinearLayoutManager(this)
    adapter.setOnRadioPressedListener(this)
  }

  private fun setupEmptyView() {
    emptyViewTitle.setText(R.string.radio__no_radio_stations)
    emptyViewIcon.setImageResource(R.drawable.ic_radio_black_80dp)
  }

  override fun onDestroy() {
    presenter.detach()
    adapter.setOnRadioPressedListener(null)

    if (isFinishing) {
      Toothpick.closeScope(PRESENTER_SCOPE)
    }
    Toothpick.closeScope(this)
    super.onDestroy()
  }

  override fun update(data: PagedList<RadioStationEntity>) {
    emptyView.isVisible = data.isEmpty()
    adapter.submitList(data)
  }

  override fun error(error: Throwable) {
    showSnackbar(R.string.radio__loading_failed)
  }

  override fun onRadioPressed(path: String) {
    presenter.play(path)
  }

  override fun onRefresh() {
    presenter.refresh()
  }

  override fun radioPlayFailed(error: Throwable?) {
    showSnackbar(R.string.radio__play_failed)
  }

  override fun radioPlaySuccessful() {
    showSnackbar(R.string.radio__play_successful)
  }

  override fun loading(visible: Boolean) {
    if (!visible) {
      emptyViewProgress.isVisible = false
      swipeLayout.isRefreshing = false
    }
  }

  @javax.inject.Scope
  @Retention(AnnotationRetention.RUNTIME)
  annotation class Presenter

  companion object {
    private val PRESENTER_SCOPE: Class<*> = Presenter::class.java
  }
}