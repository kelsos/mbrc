package com.kelsos.mbrc.adapters

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.kelsos.mbrc.annotations.Search
import com.kelsos.mbrc.ui.navigation.library.albums.BrowseAlbumFragment
import com.kelsos.mbrc.ui.navigation.library.artists.BrowseArtistFragment
import com.kelsos.mbrc.ui.navigation.library.gernes.BrowseGenreFragment
import com.kelsos.mbrc.ui.navigation.library.tracks.BrowseTrackFragment

class LibraryPagerAdapter(activity: FragmentActivity) : FragmentStateAdapter(activity) {

  override fun createFragment(position: Int): Fragment {
    return when (position) {
      Search.SECTION_GENRE -> BrowseGenreFragment()
      Search.SECTION_ARTIST -> BrowseArtistFragment()
      Search.SECTION_ALBUM -> BrowseAlbumFragment()
      Search.SECTION_TRACK -> BrowseTrackFragment()
      else -> throw IllegalStateException("invalid position")
    }
  }

  override fun getItemCount(): Int {
    return COUNT
  }

  companion object {
    const val COUNT = 4
  }
}
