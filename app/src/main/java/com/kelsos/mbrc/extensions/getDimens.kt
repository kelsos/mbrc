package com.kelsos.mbrc.extensions

import android.content.Context

fun Context.getDimens(): Int {
  val displayMetrics = resources.displayMetrics
  val dpHeight = displayMetrics.heightPixels / displayMetrics.density
  val dpWidth = displayMetrics.widthPixels / displayMetrics.density
  if (dpHeight > dpWidth) {
    return dpWidth.toInt()
  } else {
    return dpHeight.toInt()
  }
}
