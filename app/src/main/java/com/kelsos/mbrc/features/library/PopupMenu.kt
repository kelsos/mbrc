package com.kelsos.mbrc.features.library

import android.view.View
import androidx.annotation.MenuRes
import androidx.appcompat.widget.PopupMenu

fun View.popupMenu(
  @MenuRes menu: Int,
  onSelect: (itemId: Int) -> Unit,
) {
  val popupMenu = PopupMenu(this.context, this)

  popupMenu.inflate(menu)
  popupMenu.setOnMenuItemClickListener {
    onSelect(it.itemId)
    true
  }
  popupMenu.show()
}
