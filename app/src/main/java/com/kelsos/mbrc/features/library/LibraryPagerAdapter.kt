package com.kelsos.mbrc.features.library

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.kelsos.mbrc.annotations.Search

class LibraryPagerAdapter(
  activity: FragmentActivity,
) : FragmentStateAdapter(activity) {
  override fun createFragment(position: Int): Fragment =
    when (position) {
      Search.SECTION_GENRE -> BrowseGenreFragment()
      Search.SECTION_ARTIST -> BrowseArtistFragment()
      Search.SECTION_ALBUM -> BrowseAlbumFragment()
      Search.SECTION_TRACK -> BrowseTrackFragment()
      else -> throw IllegalStateException("invalid position")
    }

  override fun getItemCount(): Int = COUNT

  companion object {
    const val COUNT = 4
  }
}
