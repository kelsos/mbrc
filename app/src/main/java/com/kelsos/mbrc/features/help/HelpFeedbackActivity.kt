package com.kelsos.mbrc.features.help

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator
import com.kelsos.mbrc.R

class HelpFeedbackActivity : AppCompatActivity() {
  private lateinit var toolbar: MaterialToolbar
  private lateinit var tabLayout: TabLayout
  private lateinit var viewPager: ViewPager2

  private lateinit var pagerAdapter: HelpFeedbackPagerAdapter

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setContentView(R.layout.activity_help_feedback)

    toolbar = findViewById(R.id.toolbar)
    tabLayout = findViewById(R.id.feedback_tab_layout)
    viewPager = findViewById(R.id.pager_help_feedback)

    setSupportActionBar(toolbar)
    val actionBar = supportActionBar

    if (actionBar != null) {
      actionBar.setDisplayHomeAsUpEnabled(true)
      actionBar.setHomeButtonEnabled(true)
    }

    pagerAdapter = HelpFeedbackPagerAdapter(this)
    viewPager.adapter = pagerAdapter
    TabLayoutMediator(tabLayout, viewPager) { currentTab, currentPosition ->
      currentTab.text =
        when (currentPosition) {
          HelpFeedbackPagerAdapter.HELP -> getString(R.string.tab_help)
          HelpFeedbackPagerAdapter.FEEDBACK -> getString(R.string.common_feedback)
          else -> throw IllegalArgumentException("invalid position")
        }
    }.attach()
  }
}
