package com.kelsos.mbrc.features.nowplaying

class MoveManagerImpl : MoveManager {
  private var originalPosition: Int = -1
  private var finalPosition: Int = -1

  private lateinit var onMoveSubmit: (originalPosition: Int, finalPosition: Int) -> Unit

  override fun onMoveCommit(onMoveSubmit: (originalPosition: Int, finalPosition: Int) -> Unit) {
    this.onMoveSubmit = onMoveSubmit
  }

  override fun commit() {
    onMoveSubmit(originalPosition, finalPosition)
    originalPosition = -1
    finalPosition = -1
  }

  override fun move(
    from: Int,
    to: Int,
  ) {
    if (originalPosition < 0) {
      originalPosition = from
    }

    if (finalPosition != to) {
      finalPosition = to
    }
  }
}
