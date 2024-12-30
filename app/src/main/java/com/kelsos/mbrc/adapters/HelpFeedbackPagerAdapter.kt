package com.kelsos.mbrc.adapters

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.kelsos.mbrc.ui.help_feedback.FeedbackFragment
import com.kelsos.mbrc.ui.help_feedback.HelpFragment

class HelpFeedbackPagerAdapter(activity: FragmentActivity) : FragmentStateAdapter(activity) {

  override fun createFragment(position: Int): Fragment {
    return when (position) {
      HELP -> HelpFragment.newInstance()
      FEEDBACK -> FeedbackFragment.newInstance()
      else -> throw IllegalArgumentException("invalid position")
    }
  }

  override fun getItemCount(): Int {
    return PAGES
  }

  companion object {
    const val HELP = 0
    const val FEEDBACK = 1
    private const val PAGES = 2
  }
}
