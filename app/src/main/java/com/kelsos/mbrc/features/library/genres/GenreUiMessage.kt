package com.kelsos.mbrc.features.library.genres

import com.kelsos.mbrc.common.mvvm.UiMessageBase

sealed class GenreUiMessage : UiMessageBase {
  class OpenArtists(
    val genre: Genre,
  ) : GenreUiMessage()

  class QueueSuccess(
    val tracksCount: Int,
  ) : GenreUiMessage()

  object QueueFailed : GenreUiMessage()
}
