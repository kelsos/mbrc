package com.kelsos.mbrc.features.library.albums

import com.kelsos.mbrc.common.mvvm.UiMessageBase

sealed class AlbumUiMessage : UiMessageBase {
  data class OpenAlbumTracks(
    val album: Album,
  ) : AlbumUiMessage()

  data class QueueSuccess(
    val tracksCount: Int,
  ) : AlbumUiMessage()

  object QueueFailed : AlbumUiMessage()

  object NetworkUnavailable : AlbumUiMessage()
}
