package com.kelsos.mbrc.features.library

import com.kelsos.mbrc.common.mvvm.UiMessageBase

sealed class LibraryUiEvent : UiMessageBase {
  class LibraryStatsReady(
    val stats: LibraryStats,
  ) : LibraryUiEvent()

  class LibrarySyncComplete(
    val stats: LibraryStats,
  ) : LibraryUiEvent()

  class LibrarySyncFailed(
    val message: String,
  ) : LibraryUiEvent()

  class UpdateAlbumArtistOnly(
    val enabled: Boolean,
  ) : LibraryUiEvent()
}
