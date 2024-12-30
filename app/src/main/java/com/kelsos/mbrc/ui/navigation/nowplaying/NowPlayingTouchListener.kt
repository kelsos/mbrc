package com.kelsos.mbrc.ui.navigation.nowplaying

import android.content.Context
import android.view.GestureDetector
import android.view.MotionEvent
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.OnItemTouchListener
import timber.log.Timber

class NowPlayingTouchListener(
  context: Context,
  private val onLongClick: (Boolean) -> Unit,
) : OnItemTouchListener {
  private val gestureDetector: GestureDetector =
    GestureDetector(
      context,
      object : GestureDetector.SimpleOnGestureListener() {
        override fun onLongPress(e: MotionEvent) {
          Timber.v("Marking start of long press event")
          onLongClick.invoke(true)
          super.onLongPress(e)
        }
      },
    )

  override fun onTouchEvent(
    rv: RecyclerView,
    e: MotionEvent,
  ) {
  }

  override fun onInterceptTouchEvent(
    rv: RecyclerView,
    e: MotionEvent,
  ): Boolean {
    gestureDetector.onTouchEvent(e)
    val action = e.actionMasked
    if (action == MotionEvent.ACTION_UP) {
      Timber.v("Marking end of long press event")
      onLongClick.invoke(false)
    }

    return false
  }

  override fun onRequestDisallowInterceptTouchEvent(disallowIntercept: Boolean) {
  }
}
