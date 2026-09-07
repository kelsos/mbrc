package com.kelsos.mbrc.core.common.settings

import com.kelsos.mbrc.core.common.layout.WindowWidthClass

enum class AlbumViewMode(val value: String) {
  LIST("list"),
  GRID("grid"),
  AUTO("auto");

  fun isGrid(screenWidthDp: Int): Boolean = when (this) {
    AUTO -> WindowWidthClass.fromWidthDp(screenWidthDp) != WindowWidthClass.Compact
    GRID -> true
    LIST -> false
  }

  companion object {
    fun fromString(value: String): AlbumViewMode = entries.find { it.value == value } ?: AUTO
  }
}
