package com.kelsos.mbrc.features.library.genres

import com.kelsos.mbrc.common.mvvm.UiMessageBase

sealed class GenreUiMessage : UiMessageBase {
  data class OpenArtists(
    val genre: Genre,
  ) : GenreUiMessage()

  data class QueueSuccess(
    val tracksCount: Int,
  ) : GenreUiMessage()

  object QueueFailed : GenreUiMessage()

  object NetworkUnavailable : GenreUiMessage()
}
