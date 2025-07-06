package com.kelsos.mbrc

import android.os.Bundle
import android.view.MenuItem
import androidx.activity.OnBackPressedCallback
import androidx.annotation.LayoutRes
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import com.google.android.material.appbar.AppBarLayout
import com.google.android.material.appbar.MaterialToolbar
import org.koin.androidx.scope.ScopeActivity

open class CommonToolbarActivity(@LayoutRes contentLayoutId: Int) :
  ScopeActivity(contentLayoutId) {
  protected lateinit var toolbar: MaterialToolbar

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)

    setupAppBarUnderStatusBar()
    setupToolbar()
    setupBackPressHandler()
    setupNavigationBarInsets()
  }

  private fun setupToolbar() {
    toolbar = findViewById(R.id.toolbar)
    setSupportActionBar(toolbar)
    supportActionBar?.setDisplayHomeAsUpEnabled(true)
  }

  private fun setupBackPressHandler() {
    onBackPressedDispatcher.addCallback(
      this,
      object : OnBackPressedCallback(true) {
        override fun handleOnBackPressed() {
          finish()
        }
      }
    )
  }

  private fun setupNavigationBarInsets() {
    val rootView = findViewById<android.view.View>(android.R.id.content)
    ViewCompat.setOnApplyWindowInsetsListener(rootView) { view, insets ->
      val navigationBarInsets = insets.getInsets(WindowInsetsCompat.Type.navigationBars())
      view.updatePadding(bottom = navigationBarInsets.bottom)
      insets
    }
  }

  override fun onOptionsItemSelected(item: MenuItem): Boolean {
    if (item.itemId == android.R.id.home) {
      onBackPressedDispatcher.onBackPressed()
      return true
    } else {
      return super.onOptionsItemSelected(item)
    }
  }

  protected fun setToolbarTitle(title: String) {
    supportActionBar?.title = title.ifEmpty { getString(R.string.empty) }
  }

  protected fun setToolbarTitle(titleRes: Int) {
    supportActionBar?.setTitle(titleRes)
  }

  private fun setupAppBarUnderStatusBar() {
    val appBarLayout = findViewById<AppBarLayout>(R.id.app_bar_layout)
    ViewCompat.setOnApplyWindowInsetsListener(appBarLayout) { view, insets ->
      val statusBarInsets = insets.getInsets(WindowInsetsCompat.Type.statusBars())
      view.updatePadding(top = statusBarInsets.top)
      insets
    }
  }
}
