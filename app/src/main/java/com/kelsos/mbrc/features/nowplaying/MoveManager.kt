package com.kelsos.mbrc.features.nowplaying

interface MoveManager {
  fun move(
    from: Int,
    to: Int,
  )

  fun commit()

  fun onMoveCommit(onMoveSubmit: (originalPosition: Int, finalPosition: Int) -> Unit)
}
