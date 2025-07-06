package com.kelsos.mbrc.platform.mediasession

sealed class RemoteIntentCode(val code: Int) {
  object Open : RemoteIntentCode(OPEN)

  object Play : RemoteIntentCode(PLAY)

  object Next : RemoteIntentCode(NEXT)

  object Close : RemoteIntentCode(CLOSE)

  object Previous : RemoteIntentCode(PREVIOUS)

  object Cancel : RemoteIntentCode(CANCEL)

  companion object {
    const val OPEN = 0
    const val PLAY = 1
    const val NEXT = 2
    const val CLOSE = 3
    const val PREVIOUS = 4
    const val CANCEL = 5
  }
}
