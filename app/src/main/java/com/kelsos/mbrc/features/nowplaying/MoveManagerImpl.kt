package com.kelsos.mbrc.features.nowplaying

class MoveManagerImpl : MoveManager {
  private var originalPosition: Int = -1
  private var finalPosition: Int = -1

  private var onMoveSubmit: ((originalPosition: Int, finalPosition: Int) -> Unit)? = null

  override fun onMoveCommit(onMoveSubmit: (originalPosition: Int, finalPosition: Int) -> Unit) {
    this.onMoveSubmit = onMoveSubmit
  }

  override fun commit() {
    val submissionHandler = onMoveSubmit
    if (hasValidMove() && submissionHandler != null) {
      submissionHandler(originalPosition, finalPosition)
      reset()
    }
  }

  override fun move(from: Int, to: Int) {
    require(from >= 0) { "from position must be non-negative" }
    require(to >= 0) { "to position must be non-negative" }
    if (originalPosition < 0) {
      originalPosition = from
    }

    if (finalPosition != to) {
      finalPosition = to
    }
  }

  private fun hasValidMove(): Boolean =
    originalPosition >= 0 && finalPosition >= 0 && originalPosition != finalPosition

  private fun reset() {
    originalPosition = -1
    finalPosition = -1
  }
}
