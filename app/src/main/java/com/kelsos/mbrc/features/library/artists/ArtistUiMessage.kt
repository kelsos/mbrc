package com.kelsos.mbrc.features.library.artists

import com.kelsos.mbrc.common.mvvm.UiMessageBase

sealed class ArtistUiMessage : UiMessageBase {
  data class OpenArtistAlbums(
    val artist: Artist,
  ) : ArtistUiMessage()

  object QueueFailed : ArtistUiMessage()

  data class QueueSuccess(
    val tracksCount: Int,
  ) : ArtistUiMessage()

  object NetworkUnavailable : ArtistUiMessage()
}
