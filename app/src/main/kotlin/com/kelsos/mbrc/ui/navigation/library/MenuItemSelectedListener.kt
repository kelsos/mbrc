package com.kelsos.mbrc.ui.navigation.library

import com.kelsos.mbrc.content.nowplaying.queue.LibraryPopup

interface MenuItemSelectedListener<in T> {

  fun onMenuItemSelected(@LibraryPopup.Action action: String, item: T)

  fun onItemClicked(item: T)
}