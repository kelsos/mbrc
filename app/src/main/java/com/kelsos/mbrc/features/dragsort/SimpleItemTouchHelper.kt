package com.kelsos.mbrc.features.dragsort

import android.graphics.Canvas
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.RecyclerView

class SimpleItemTouchHelper(private val adapter: ItemTouchHelperAdapter) :
  ItemTouchHelper.Callback() {
  override fun getMovementFlags(
    recyclerView: RecyclerView,
    viewHolder: RecyclerView.ViewHolder
  ): Int {
    val dragFlags = ItemTouchHelper.UP or ItemTouchHelper.DOWN
    val swipeFlags = ItemTouchHelper.END
    return makeMovementFlags(dragFlags, swipeFlags)
  }

  override fun onMove(
    recyclerView: RecyclerView,
    viewHolder: RecyclerView.ViewHolder,
    target: RecyclerView.ViewHolder
  ): Boolean {
    if (viewHolder.itemViewType != target.itemViewType) {
      return false
    }

    // Notify the adapter of the move
    adapter.onItemMove(viewHolder.bindingAdapterPosition, target.bindingAdapterPosition)
    return true
  }

  override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
    adapter.onItemDismiss(viewHolder.bindingAdapterPosition)
  }

  override fun isLongPressDragEnabled(): Boolean = false

  override fun isItemViewSwipeEnabled(): Boolean = true

  override fun onChildDrawOver(
    c: Canvas,
    recyclerView: RecyclerView,
    viewHolder: RecyclerView.ViewHolder,
    dX: Float,
    dY: Float,
    actionState: Int,
    isCurrentlyActive: Boolean
  ) {
    if (actionState == ItemTouchHelper.ACTION_STATE_SWIPE) {
      // Fade out the view as it is swiped out of the parent's bounds
      val alpha = ALPHA_FULL - Math.abs(dX) / viewHolder.itemView.width.toFloat()
      viewHolder.itemView.alpha = alpha
      viewHolder.itemView.translationX = dX
    } else {
      super.onChildDraw(c, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive)
    }
  }

  override fun onSelectedChanged(viewHolder: RecyclerView.ViewHolder?, actionState: Int) {
    if (actionState != ItemTouchHelper.ACTION_STATE_IDLE && viewHolder is TouchHelperViewHolder) {
      viewHolder.onItemSelected()
    }

    super.onSelectedChanged(viewHolder, actionState)
  }

  override fun clearView(recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder) {
    super.clearView(recyclerView, viewHolder)
    viewHolder.itemView.alpha = ALPHA_FULL

    if (viewHolder is TouchHelperViewHolder) {
      viewHolder.onItemClear()
    }
  }

  companion object {
    const val ALPHA_FULL = 1.0F
  }
}
