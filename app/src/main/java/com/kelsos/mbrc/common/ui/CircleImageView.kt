/*
 * Copyright 2014 - 2015 Henning Dodenhof
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.kelsos.mbrc.common.ui

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapShader
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.ColorFilter
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.Drawable
import android.net.Uri
import android.util.AttributeSet
import androidx.annotation.ColorInt
import androidx.annotation.ColorRes
import androidx.annotation.DrawableRes
import androidx.appcompat.widget.AppCompatImageView
import androidx.core.content.ContextCompat
import androidx.core.content.withStyledAttributes
import androidx.core.graphics.createBitmap
import com.kelsos.mbrc.R
import timber.log.Timber

class CircleImageView : AppCompatImageView {
  private val drawableRect = RectF()
  private val borderRect = RectF()

  private val shaderMatrix = Matrix()
  private val bitmapPaint = Paint()
  private val borderPaint = Paint()
  private val fillPaint = Paint()

  private var mBorderColor = DEFAULT_BORDER_COLOR
  private var mBorderWidth = DEFAULT_BORDER_WIDTH
  private var mFillColor = DEFAULT_FILL_COLOR

  private var bitmap: Bitmap? = null
  private var mBitmapShader: BitmapShader? = null
  private var mBitmapWidth: Int = 0
  private var mBitmapHeight: Int = 0

  private var mDrawableRadius: Float = 0.toFloat()
  private var mBorderRadius: Float = 0.toFloat()

  private var mColorFilter: ColorFilter? = null

  private var mReady: Boolean = false
  private var mSetupPending: Boolean = false
  private var mBorderOverlay: Boolean = false
  var isDisableCircularTransformation: Boolean = false
    set(disableCircularTransformation) {
      if (isDisableCircularTransformation == disableCircularTransformation) {
        return
      }

      field = disableCircularTransformation
      initializeBitmap()
    }

  constructor(context: Context) : super(context) {
    init()
  }

  @JvmOverloads
  constructor(context: Context, attrs: AttributeSet, defStyle: Int = 0) : super(
    context,
    attrs,
    defStyle
  ) {
    context.withStyledAttributes(attrs, R.styleable.CircleImageView, defStyle, 0) {
      mBorderWidth =
        getDimensionPixelSize(R.styleable.CircleImageView_civ_border_width, DEFAULT_BORDER_WIDTH)
      mBorderColor = getColor(R.styleable.CircleImageView_civ_border_color, DEFAULT_BORDER_COLOR)
      mBorderOverlay =
        getBoolean(R.styleable.CircleImageView_civ_border_overlay, DEFAULT_BORDER_OVERLAY)
      mFillColor = getColor(R.styleable.CircleImageView_civ_fill_color, DEFAULT_FILL_COLOR)
    }

    init()
  }

  private fun init() {
    super.setScaleType(SCALE_TYPE)
    mReady = true

    if (mSetupPending) {
      setup()
      mSetupPending = false
    }
  }

  override fun getScaleType(): ScaleType = SCALE_TYPE

  override fun setScaleType(scaleType: ScaleType) {
    require(scaleType == SCALE_TYPE) { "ScaleType $scaleType not supported." }
  }

  override fun setAdjustViewBounds(adjustViewBounds: Boolean) {
    require(!adjustViewBounds) { "adjustViewBounds not supported." }
  }

  override fun onDraw(canvas: Canvas) {
    if (isDisableCircularTransformation) {
      super.onDraw(canvas)
      return
    }

    if (bitmap == null) {
      return
    }

    if (mFillColor != Color.TRANSPARENT) {
      canvas.drawCircle(
        drawableRect.centerX(),
        drawableRect.centerY(),
        mDrawableRadius,
        fillPaint
      )
    }
    canvas.drawCircle(
      drawableRect.centerX(),
      drawableRect.centerY(),
      mDrawableRadius,
      bitmapPaint
    )
    if (mBorderWidth != 0) {
      canvas.drawCircle(borderRect.centerX(), borderRect.centerY(), mBorderRadius, borderPaint)
    }
  }

  override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
    super.onSizeChanged(w, h, oldw, oldh)
    setup()
  }

  var borderColor: Int
    get() = mBorderColor
    set(
      @ColorInt borderColor
    ) {
      if (borderColor == mBorderColor) {
        return
      }

      mBorderColor = borderColor
      borderPaint.color = mBorderColor
      invalidate()
    }

  fun setBorderColorResource(@ColorRes borderColorRes: Int) {
    borderColor = ContextCompat.getColor(context, borderColorRes)
  }

  var fillColor: Int
    get() = mFillColor
    set(
      @ColorInt fillColor
    ) {
      if (fillColor == mFillColor) {
        return
      }

      mFillColor = fillColor
      fillPaint.color = fillColor
      invalidate()
    }

  fun setFillColorResource(@ColorRes fillColorRes: Int) {
    fillColor = ContextCompat.getColor(context, fillColorRes)
  }

  var borderWidth: Int
    get() = mBorderWidth
    set(borderWidth) {
      if (borderWidth == mBorderWidth) {
        return
      }

      mBorderWidth = borderWidth
      setup()
    }

  var isBorderOverlay: Boolean
    get() = mBorderOverlay
    set(borderOverlay) {
      if (borderOverlay == mBorderOverlay) {
        return
      }

      mBorderOverlay = borderOverlay
      setup()
    }

  override fun setImageBitmap(bm: Bitmap) {
    super.setImageBitmap(bm)
    initializeBitmap()
  }

  override fun setImageDrawable(drawable: Drawable?) {
    super.setImageDrawable(drawable)
    initializeBitmap()
  }

  override fun setImageResource(@DrawableRes resId: Int) {
    super.setImageResource(resId)
    initializeBitmap()
  }

  override fun setImageURI(uri: Uri?) {
    super.setImageURI(uri)
    initializeBitmap()
  }

  override fun setColorFilter(cf: ColorFilter) {
    if (cf === mColorFilter) {
      return
    }

    mColorFilter = cf
    applyColorFilter()
    invalidate()
  }

  override fun getColorFilter(): ColorFilter = requireNotNull(mColorFilter)

  private fun applyColorFilter() {
    bitmapPaint.colorFilter = mColorFilter
  }

  private fun getBitmapFromDrawable(drawable: Drawable?): Bitmap? {
    if (drawable == null) {
      return null
    }

    if (drawable is BitmapDrawable) {
      return drawable.bitmap
    }

    try {
      val bitmap: Bitmap =
        if (drawable is ColorDrawable) {
          createBitmap(COLOR_DRAWABLE_DIMENSION, COLOR_DRAWABLE_DIMENSION, BITMAP_CONFIG)
        } else {
          createBitmap(drawable.intrinsicWidth, drawable.intrinsicHeight, BITMAP_CONFIG)
        }

      val canvas = Canvas(bitmap)
      drawable.setBounds(0, 0, canvas.width, canvas.height)
      drawable.draw(canvas)
      return bitmap
    } catch (e: IllegalArgumentException) {
      Timber.e(e)
      return null
    }
  }

  private fun initializeBitmap() {
    if (isDisableCircularTransformation) {
      bitmap = null
    } else {
      bitmap = getBitmapFromDrawable(drawable)
    }
    setup()
  }

  private fun setup() {
    if (!mReady) {
      mSetupPending = true
      return
    }

    if (width == 0 && height == 0) {
      return
    }

    val bmp = bitmap
    if (bmp == null) {
      invalidate()
      return
    }

    mBitmapShader = BitmapShader(bmp, Shader.TileMode.CLAMP, Shader.TileMode.CLAMP)

    bitmapPaint.isAntiAlias = true
    bitmapPaint.shader = mBitmapShader

    borderPaint.style = Paint.Style.STROKE
    borderPaint.isAntiAlias = true
    borderPaint.color = mBorderColor
    borderPaint.strokeWidth = mBorderWidth.toFloat()

    fillPaint.style = Paint.Style.FILL
    fillPaint.isAntiAlias = true
    fillPaint.color = mFillColor

    mBitmapHeight = bmp.height
    mBitmapWidth = bmp.width

    borderRect.set(calculateBounds())
    mBorderRadius =
      ((borderRect.height() - mBorderWidth) / 2.0f).coerceAtMost(
        (borderRect.width() - mBorderWidth) / 2.0f
      )

    drawableRect.set(borderRect)
    if (!mBorderOverlay) {
      drawableRect.inset(mBorderWidth.toFloat(), mBorderWidth.toFloat())
    }
    mDrawableRadius = (drawableRect.height() / 2.0f).coerceAtMost(drawableRect.width() / 2.0f)

    applyColorFilter()
    updateShaderMatrix()
    invalidate()
  }

  private fun calculateBounds(): RectF {
    val availableWidth = width - paddingLeft - paddingRight
    val availableHeight = height - paddingTop - paddingBottom

    val sideLength = availableWidth.coerceAtMost(availableHeight)

    val left = paddingLeft + (availableWidth - sideLength) / 2f
    val top = paddingTop + (availableHeight - sideLength) / 2f

    return RectF(left, top, left + sideLength, top + sideLength)
  }

  private fun updateShaderMatrix() {
    val scale: Float
    var dx = 0f
    var dy = 0f

    shaderMatrix.set(null)

    if (mBitmapWidth * drawableRect.height() > drawableRect.width() * mBitmapHeight) {
      scale = drawableRect.height() / mBitmapHeight.toFloat()
      dx = (drawableRect.width() - mBitmapWidth * scale) * HALF
    } else {
      scale = drawableRect.width() / mBitmapWidth.toFloat()
      dy = (drawableRect.height() - mBitmapHeight * scale) * HALF
    }

    shaderMatrix.setScale(scale, scale)
    shaderMatrix.postTranslate(
      (dx + HALF).toInt() + drawableRect.left,
      (dy + HALF).toInt() + drawableRect.top
    )

    requireNotNull(mBitmapShader).setLocalMatrix(shaderMatrix)
  }

  companion object {
    private val SCALE_TYPE = ScaleType.CENTER_CROP

    private val BITMAP_CONFIG = Bitmap.Config.ARGB_8888
    private const val COLOR_DRAWABLE_DIMENSION = 2

    private const val DEFAULT_BORDER_WIDTH = 0
    private const val DEFAULT_BORDER_COLOR = Color.BLACK
    private const val DEFAULT_FILL_COLOR = Color.TRANSPARENT
    private const val DEFAULT_BORDER_OVERLAY = false
    private const val HALF = 0.5f
  }
}
