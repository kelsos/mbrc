package com.kelsos.mbrc.features.queue

import androidx.annotation.StringDef

object Queue {
  @StringDef(
    NEXT,
    LAST,
    NOW,
    ADD_ALL,
    ADD_ALBUM,
    DEFAULT
  )
  @Retention(AnnotationRetention.SOURCE)
  annotation class Action

  const val NEXT = "next"
  const val LAST = "last"
  const val NOW = "now"
  const val ADD_ALL = "add-all"
  const val ADD_ALBUM = "add-album"
  const val DEFAULT = "default"
}