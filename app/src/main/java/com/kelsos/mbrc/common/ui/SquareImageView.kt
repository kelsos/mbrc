package com.kelsos.mbrc.common.ui

import android.content.Context
import android.util.AttributeSet
import androidx.appcompat.widget.AppCompatImageView

class SquareImageView : AppCompatImageView {
  constructor(context: Context) : super(context)

  constructor(context: Context, attrs: AttributeSet) : super(context, attrs)

  override fun onMeasure(
    widthMeasureSpec: Int,
    heightMeasureSpec: Int,
  ) {
    super.onMeasure(widthMeasureSpec, heightMeasureSpec)
    setMeasuredDimension(measuredHeight, measuredHeight)
  }
}
