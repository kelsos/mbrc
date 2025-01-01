package com.kelsos.mbrc.features.output

sealed class OutputSelectionResult {
  object Success : OutputSelectionResult()

  object ConnectionError : OutputSelectionResult()

  object UnknownError : OutputSelectionResult()
}
