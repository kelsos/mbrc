package com.kelsos.mbrc.common.ui

import android.content.Context
import android.util.AttributeSet
import android.widget.ImageButton
import com.kelsos.mbrc.R

class SquareImageButton : ImageButton {
  private var useHeight: Boolean = false

  constructor(context: Context) : super(context)

  constructor(context: Context, attrs: AttributeSet) : super(context, attrs) {
    val attributes = context.obtainStyledAttributes(attrs, R.styleable.SquareImageButton)
    useHeight = attrs.getAttributeBooleanValue(R.styleable.SquareImageButton_sib_use_height, false)
    attributes.recycle()
  }

  override fun onMeasure(
    widthMeasureSpec: Int,
    heightMeasureSpec: Int,
  ) {
    super.onMeasure(widthMeasureSpec, heightMeasureSpec)
    if (useHeight) {
      setMeasuredDimension(measuredHeight, measuredHeight)
    } else {
      setMeasuredDimension(measuredWidth, measuredWidth)
    }
  }
}
