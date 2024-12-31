package com.kelsos.mbrc.features.library

import kotlinx.coroutines.flow.MutableStateFlow
import javax.inject.Inject

class LibrarySearchModel
  @Inject
  constructor() {
    val term: MutableStateFlow<String> = MutableStateFlow("")
  }
