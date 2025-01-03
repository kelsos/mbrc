package com.kelsos.mbrc.common.ui

import android.content.Context
import android.util.AttributeSet
import androidx.appcompat.widget.AppCompatImageButton
import androidx.core.content.withStyledAttributes
import com.kelsos.mbrc.R

class SquareImageButton : AppCompatImageButton {
  private var useHeight: Boolean = false

  constructor(context: Context) : super(context)

  constructor(context: Context, attrs: AttributeSet) : super(context, attrs) {
    context.withStyledAttributes(attrs, R.styleable.SquareImageButton) {
      useHeight = attrs.getAttributeBooleanValue(R.styleable.SquareImageButton_sib_use_height, false)
    }
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
