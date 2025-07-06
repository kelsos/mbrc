package com.kelsos.mbrc.common.ui

import android.content.Context
import android.util.AttributeSet
import android.view.View
import androidx.recyclerview.widget.RecyclerView

class EmptyRecyclerView : RecyclerView {
  var emptyView: View? = null
    set(value) {
      field = value
      checkIfEmpty()
    }

  constructor(context: Context) : super(context)

  constructor(context: Context, attrs: AttributeSet) : super(context, attrs)

  constructor(context: Context, attrs: AttributeSet, defStyle: Int) : super(
    context,
    attrs,
    defStyle
  )

  internal fun checkIfEmpty() {
    val adapter = adapter
    emptyView?.visibility =
      if (adapter != null && adapter.itemCount > 0) GONE else VISIBLE
  }

  internal val observer: AdapterDataObserver =
    object : AdapterDataObserver() {
      override fun onChanged() {
        super.onChanged()
        checkIfEmpty()
      }
    }

  override fun setAdapter(adapter: Adapter<*>?) {
    val oldAdapter = getAdapter()
    oldAdapter?.unregisterAdapterDataObserver(observer)
    super.setAdapter(adapter)
    adapter?.registerAdapterDataObserver(observer)
  }
}
