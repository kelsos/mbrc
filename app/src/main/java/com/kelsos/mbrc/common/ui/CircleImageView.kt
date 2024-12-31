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
import android.widget.ImageView
import androidx.annotation.ColorInt
import androidx.annotation.ColorRes
import androidx.annotation.DrawableRes
import androidx.core.content.ContextCompat
import com.kelsos.mbrc.R

class CircleImageView : ImageView {
  private val mDrawableRect = RectF()
  private val mBorderRect = RectF()

  private val mShaderMatrix = Matrix()
  private val mBitmapPaint = Paint()
  private val mBorderPaint = Paint()
  private val mFillPaint = Paint()

  private var mBorderColor = DEFAULT_BORDER_COLOR
  private var mBorderWidth = DEFAULT_BORDER_WIDTH
  private var mFillColor = DEFAULT_FILL_COLOR

  private var mBitmap: Bitmap? = null
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
    defStyle,
  ) {

    val a = context.obtainStyledAttributes(attrs, R.styleable.CircleImageView, defStyle, 0)

    mBorderWidth =
      a.getDimensionPixelSize(R.styleable.CircleImageView_civ_border_width, DEFAULT_BORDER_WIDTH)
    mBorderColor = a.getColor(R.styleable.CircleImageView_civ_border_color, DEFAULT_BORDER_COLOR)
    mBorderOverlay =
      a.getBoolean(R.styleable.CircleImageView_civ_border_overlay, DEFAULT_BORDER_OVERLAY)
    mFillColor = a.getColor(R.styleable.CircleImageView_civ_fill_color, DEFAULT_FILL_COLOR)

    a.recycle()

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
    if (scaleType != SCALE_TYPE) {
      throw IllegalArgumentException(String.format("ScaleType %s not supported.", scaleType))
    }
  }

  override fun setAdjustViewBounds(adjustViewBounds: Boolean) {
    if (adjustViewBounds) {
      throw IllegalArgumentException("adjustViewBounds not supported.")
    }
  }

  override fun onDraw(canvas: Canvas) {
    if (isDisableCircularTransformation) {
      super.onDraw(canvas)
      return
    }

    if (mBitmap == null) {
      return
    }

    if (mFillColor != Color.TRANSPARENT) {
      canvas.drawCircle(
        mDrawableRect.centerX(),
        mDrawableRect.centerY(),
        mDrawableRadius,
        mFillPaint,
      )
    }
    canvas.drawCircle(
      mDrawableRect.centerX(),
      mDrawableRect.centerY(),
      mDrawableRadius,
      mBitmapPaint,
    )
    if (mBorderWidth != 0) {
      canvas.drawCircle(mBorderRect.centerX(), mBorderRect.centerY(), mBorderRadius, mBorderPaint)
    }
  }

  override fun onSizeChanged(
    w: Int,
    h: Int,
    oldw: Int,
    oldh: Int,
  ) {
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
      mBorderPaint.color = mBorderColor
      invalidate()
    }

  fun setBorderColorResource(
    @ColorRes borderColorRes: Int,
  ) {
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
      mFillPaint.color = fillColor
      invalidate()
    }

  fun setFillColorResource(
    @ColorRes fillColorRes: Int,
  ) {
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

  override fun setImageResource(
    @DrawableRes resId: Int,
  ) {
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

  override fun getColorFilter(): ColorFilter = mColorFilter!!

  private fun applyColorFilter() {
    mBitmapPaint.colorFilter = mColorFilter
  }

  private fun getBitmapFromDrawable(drawable: Drawable?): Bitmap? {
    if (drawable == null) {
      return null
    }

    if (drawable is BitmapDrawable) {
      return drawable.bitmap
    }

    try {
      val bitmap: Bitmap

      if (drawable is ColorDrawable) {
        bitmap =
          Bitmap.createBitmap(COLORDRAWABLE_DIMENSION, COLORDRAWABLE_DIMENSION, BITMAP_CONFIG)
      } else {
        bitmap =
          Bitmap.createBitmap(drawable.intrinsicWidth, drawable.intrinsicHeight, BITMAP_CONFIG)
      }

      val canvas = Canvas(bitmap)
      drawable.setBounds(0, 0, canvas.width, canvas.height)
      drawable.draw(canvas)
      return bitmap
    } catch (e: Exception) {
      e.printStackTrace()
      return null
    }
  }

  private fun initializeBitmap() {
    if (isDisableCircularTransformation) {
      mBitmap = null
    } else {
      mBitmap = getBitmapFromDrawable(drawable)
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

    if (mBitmap == null) {
      invalidate()
      return
    }

    mBitmapShader = BitmapShader(mBitmap!!, Shader.TileMode.CLAMP, Shader.TileMode.CLAMP)

    mBitmapPaint.isAntiAlias = true
    mBitmapPaint.shader = mBitmapShader

    mBorderPaint.style = Paint.Style.STROKE
    mBorderPaint.isAntiAlias = true
    mBorderPaint.color = mBorderColor
    mBorderPaint.strokeWidth = mBorderWidth.toFloat()

    mFillPaint.style = Paint.Style.FILL
    mFillPaint.isAntiAlias = true
    mFillPaint.color = mFillColor

    mBitmapHeight = mBitmap!!.height
    mBitmapWidth = mBitmap!!.width

    mBorderRect.set(calculateBounds())
    mBorderRadius =
      Math.min(
        (mBorderRect.height() - mBorderWidth) / 2.0f,
        (mBorderRect.width() - mBorderWidth) / 2.0f,
      )

    mDrawableRect.set(mBorderRect)
    if (!mBorderOverlay) {
      mDrawableRect.inset(mBorderWidth.toFloat(), mBorderWidth.toFloat())
    }
    mDrawableRadius = Math.min(mDrawableRect.height() / 2.0f, mDrawableRect.width() / 2.0f)

    applyColorFilter()
    updateShaderMatrix()
    invalidate()
  }

  private fun calculateBounds(): RectF {
    val availableWidth = width - paddingLeft - paddingRight
    val availableHeight = height - paddingTop - paddingBottom

    val sideLength = Math.min(availableWidth, availableHeight)

    val left = paddingLeft + (availableWidth - sideLength) / 2f
    val top = paddingTop + (availableHeight - sideLength) / 2f

    return RectF(left, top, left + sideLength, top + sideLength)
  }

  private fun updateShaderMatrix() {
    val scale: Float
    var dx = 0f
    var dy = 0f

    mShaderMatrix.set(null)

    if (mBitmapWidth * mDrawableRect.height() > mDrawableRect.width() * mBitmapHeight) {
      scale = mDrawableRect.height() / mBitmapHeight.toFloat()
      dx = (mDrawableRect.width() - mBitmapWidth * scale) * 0.5f
    } else {
      scale = mDrawableRect.width() / mBitmapWidth.toFloat()
      dy = (mDrawableRect.height() - mBitmapHeight * scale) * 0.5f
    }

    mShaderMatrix.setScale(scale, scale)
    mShaderMatrix.postTranslate(
      (dx + 0.5f).toInt() + mDrawableRect.left,
      (dy + 0.5f).toInt() + mDrawableRect.top,
    )

    mBitmapShader!!.setLocalMatrix(mShaderMatrix)
  }

  companion object {
    private val SCALE_TYPE = ScaleType.CENTER_CROP

    private val BITMAP_CONFIG = Bitmap.Config.ARGB_8888
    private val COLORDRAWABLE_DIMENSION = 2

    private val DEFAULT_BORDER_WIDTH = 0
    private val DEFAULT_BORDER_COLOR = Color.BLACK
    private val DEFAULT_FILL_COLOR = Color.TRANSPARENT
    private val DEFAULT_BORDER_OVERLAY = false
  }
}
