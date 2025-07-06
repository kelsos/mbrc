package com.kelsos.mbrc.features.library

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.kelsos.mbrc.features.library.albums.BrowseAlbumFragment
import com.kelsos.mbrc.features.library.artists.BrowseArtistFragment
import com.kelsos.mbrc.features.library.genres.BrowseGenreFragment
import com.kelsos.mbrc.features.library.tracks.BrowseTrackFragment

class LibraryPagerAdapter(activity: FragmentActivity) : FragmentStateAdapter(activity) {
  override fun createFragment(position: Int): Fragment = when (position) {
    PagePosition.GENRES -> BrowseGenreFragment()
    PagePosition.ARTISTS -> BrowseArtistFragment()
    PagePosition.ALBUMS -> BrowseAlbumFragment()
    PagePosition.TRACKS -> BrowseTrackFragment()
    else -> error("invalid position")
  }

  override fun getItemCount(): Int = COUNT

  companion object {
    const val COUNT = 4
  }
}
