package com.kelsos.mbrc.common.state

data class PlayingPosition(
  val current: Duration = 0,
  val total: Duration = 0,
) {
  val totalMinutes get() = total.toMinutes()
  val currentMinutes get() = current.toMinutes()

  fun progress(): String = "$currentMinutes / $totalMinutes"
}

typealias Duration = Long

private const val SECONDS_IN_MINUTE = 60
private const val MILLIS_IN_SECONDS = 1000

fun Duration.toMinutes(): String {
  val inSeconds = this / MILLIS_IN_SECONDS
  val minutes = inSeconds / SECONDS_IN_MINUTE
  val seconds = inSeconds % SECONDS_IN_MINUTE
  return "%02d:%02d".format(minutes, seconds)
}

fun PlayingPosition?.orEmpty(): PlayingPosition = this ?: PlayingPosition()
