package com.kelsos.mbrc.ui.drag

interface ItemTouchHelperAdapter {
  fun onItemMove(from: Int, to: Int): Boolean

  fun onItemDismiss(position: Int)
}
