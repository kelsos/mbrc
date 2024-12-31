package com.kelsos.mbrc.features.output

sealed class OutputSelectionResult {
  var handled: Boolean = false

  object Success : OutputSelectionResult()

  object ConnectionError : OutputSelectionResult()

  object UnknownError : OutputSelectionResult()
}
