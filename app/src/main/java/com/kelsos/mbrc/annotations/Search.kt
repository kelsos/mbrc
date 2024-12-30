package com.kelsos.mbrc.annotations

import androidx.annotation.IntDef

object Search {
  const val SECTION_GENRE = 0
  const val SECTION_ARTIST = 1
  const val SECTION_ALBUM = 2
  const val SECTION_TRACK = 3

  @IntDef(SECTION_GENRE, SECTION_ARTIST, SECTION_ALBUM, SECTION_TRACK)
  @Retention(AnnotationRetention.SOURCE)
  annotation class Section
}
