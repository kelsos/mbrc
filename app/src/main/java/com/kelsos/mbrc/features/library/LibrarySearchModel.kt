package com.kelsos.mbrc.features.library

import kotlinx.coroutines.flow.MutableStateFlow

class LibrarySearchModel {
  val term: MutableStateFlow<String> = MutableStateFlow("")
}
