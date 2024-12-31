package com.kelsos.mbrc.features.dragsort

interface ItemTouchHelperAdapter {
  fun onItemMove(
    from: Int,
    to: Int,
  ): Boolean

  fun onItemDismiss(position: Int)
}
