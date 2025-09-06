package com.kelsos.mbrc.features.settings

sealed class TrackAction(val value: String) {
  object QueueNext : TrackAction(NEXT)

  object QueueLast : TrackAction(LAST)

  object PlayNow : TrackAction(NOW)

  object PlayNowQueueAll : TrackAction(ADD_ALL)

  companion object {
    const val NEXT = "next"
    const val LAST = "last"
    const val NOW = "now"
    const val ADD_ALL = "add-all"

    fun fromString(action: String): TrackAction = when (action) {
      NEXT -> QueueNext
      LAST -> QueueLast
      NOW -> PlayNow
      ADD_ALL -> PlayNowQueueAll
      else -> QueueLast
    }
  }
}
