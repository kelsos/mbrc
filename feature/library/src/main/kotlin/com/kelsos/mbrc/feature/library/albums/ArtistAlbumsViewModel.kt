package com.kelsos.mbrc.feature.library.albums

import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import androidx.paging.cachedIn
import com.kelsos.mbrc.core.common.settings.AlbumSortPreference
import com.kelsos.mbrc.core.common.settings.LibrarySettings
import com.kelsos.mbrc.core.common.state.ConnectionStateFlow
import com.kelsos.mbrc.core.data.library.album.Album
import com.kelsos.mbrc.core.data.library.album.AlbumRepository
import com.kelsos.mbrc.feature.library.queue.QueueHandler
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.launch

@OptIn(ExperimentalCoroutinesApi::class)
class ArtistAlbumsViewModel(
  private val repository: AlbumRepository,
  queueHandler: QueueHandler,
  private val librarySettings: LibrarySettings,
  connectionStateFlow: ConnectionStateFlow
) : BaseAlbumViewModel(queueHandler, librarySettings, connectionStateFlow) {
  private val artistFilter: MutableSharedFlow<String> = MutableSharedFlow(replay = 1)

  val sortPreference: Flow<AlbumSortPreference> = librarySettings.albumSortPreferenceFlow

  override val albums: Flow<PagingData<Album>> =
    combine(artistFilter, sortPreference) { artist, sort ->
      Triple(artist, sort.field, sort.order)
    }.flatMapLatest { (artist, field, order) ->
      repository.getAlbumsByArtist(artist, field, order)
    }.cachedIn(viewModelScope)

  fun load(artist: String) {
    viewModelScope.launch {
      artistFilter.emit(artist)
    }
  }

  fun updateSortPreference(preference: AlbumSortPreference) {
    viewModelScope.launch {
      librarySettings.setAlbumSortPreference(preference)
    }
  }
}
