package com.kelsos.mbrc.features.dragsort

import androidx.recyclerview.widget.RecyclerView

interface OnStartDragListener {
  fun onStartDrag(viewHolder: RecyclerView.ViewHolder)
}
