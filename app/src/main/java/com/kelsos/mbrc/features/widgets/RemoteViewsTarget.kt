package com.kelsos.mbrc.features.widgets

import android.appwidget.AppWidgetManager
import android.widget.RemoteViews
import androidx.annotation.IdRes
import coil3.Image
import coil3.target.Target
import coil3.toBitmap
import com.kelsos.mbrc.R
import timber.log.Timber

class RemoteViewsTarget(
  private val manager: AppWidgetManager,
  private val widget: RemoteViews,
  private val widgetIds: IntArray,
  @IdRes private val imageViewResId: Int,
) : Target {
  override fun onStart(placeholder: Image?) = setDrawable(placeholder, "start")

  override fun onError(error: Image?) = setDrawable(error, "error")

  override fun onSuccess(result: Image) = setDrawable(result, "success")

  private fun setDrawable(
    image: Image?,
    reason: String,
  ) {
    if (image == null) {
      Timber.v("No image found for widget setting placeholder, reason: $reason")
      widget.setImageViewResource(imageViewResId, R.drawable.ic_image_no_cover)
    } else {
      Timber.v("Updating image for widget, reason: $reason")
      widget.setImageViewBitmap(imageViewResId, image.toBitmap())
    }
    manager.updateAppWidget(widgetIds, widget)
  }
}
