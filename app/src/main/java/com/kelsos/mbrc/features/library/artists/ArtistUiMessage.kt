package com.kelsos.mbrc.features.library.artists

import com.kelsos.mbrc.common.mvvm.UiMessageBase

sealed class ArtistUiMessage : UiMessageBase {
  class OpenArtistAlbums(
    val artist: Artist,
  ) : ArtistUiMessage()

  object QueueFailed : ArtistUiMessage()

  class QueueSuccess(
    val tracksCount: Int,
  ) : ArtistUiMessage()
}
