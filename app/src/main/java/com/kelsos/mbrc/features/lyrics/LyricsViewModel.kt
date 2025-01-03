package com.kelsos.mbrc.features.lyrics

import androidx.lifecycle.ViewModel
import com.kelsos.mbrc.common.state.AppStateFlow
import kotlinx.coroutines.flow.Flow

class LyricsViewModel(
  appState: AppStateFlow,
) : ViewModel() {
  val lyrics: Flow<List<String>> = appState.lyrics
}
