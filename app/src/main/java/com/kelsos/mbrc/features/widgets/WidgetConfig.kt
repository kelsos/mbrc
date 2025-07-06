package com.kelsos.mbrc.features.widgets

import androidx.annotation.DimenRes
import androidx.annotation.IdRes
import androidx.annotation.LayoutRes
import kotlin.reflect.KClass

data class WidgetConfig(
  @LayoutRes
  val layout: Int,
  @DimenRes
  val imageSize: Int,
  @IdRes
  val imageId: Int,
  @IdRes
  val playButtonId: Int,
  val widgetClass: KClass<out WidgetBase>
)
