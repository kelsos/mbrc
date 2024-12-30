package com.kelsos.mbrc.annotations

import androidx.annotation.StringDef

object Queue {
  @StringDef(NEXT, LAST, NOW, ADD_ALL, PROFILE)
  @Retention(AnnotationRetention.SOURCE)
  annotation class Action

  const val PROFILE = "profile"
  const val NEXT = "next"
  const val LAST = "last"
  const val NOW = "now"
  const val ADD_ALL = "add-all"
  const val PLAY_ALBUM = "play-album"
  const val PLAY_ARTIST = "play-artist"

}
