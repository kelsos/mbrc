package com.kelsos.mbrc.features.library.albums

import com.kelsos.mbrc.common.mvvm.UiMessageBase

sealed class AlbumUiMessage : UiMessageBase {
  class OpenAlbumTracks(
    val album: Album,
  ) : AlbumUiMessage()

  class QueueSuccess(
    val tracksCount: Int,
  ) : AlbumUiMessage()

  object QueueFailed : AlbumUiMessage()
}
