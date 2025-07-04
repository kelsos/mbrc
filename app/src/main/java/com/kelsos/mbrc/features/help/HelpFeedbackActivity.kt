package com.kelsos.mbrc.features.help

import android.os.Bundle
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator
import com.kelsos.mbrc.CommonToolbarActivity
import com.kelsos.mbrc.R

class HelpFeedbackActivity : CommonToolbarActivity(R.layout.activity_help_feedback) {
  private lateinit var tabLayout: TabLayout
  private lateinit var viewPager: ViewPager2

  private lateinit var pagerAdapter: HelpFeedbackPagerAdapter

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)

    tabLayout = findViewById(R.id.feedback_tab_layout)
    viewPager = findViewById(R.id.pager_help_feedback)

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
