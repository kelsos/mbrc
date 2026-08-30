package com.kelsos.mbrc.service

/**
 * Singleton that tracks the foreground service state.
 */
object ServiceState {
  /**
   * Gates whether [ServiceCheckerImpl] starts the service, so it must be cleared synchronously in
   * `onDestroy`. Cleared late, a connect tapped just after a stop is a silent no-op.
   */
  @Volatile
  var isRunning: Boolean = false
    private set

  /**
   * Suppresses the reconnection loop while the service tears down. Set in `onDestroy` and cleared
   * both after [RemoteService.DESTROY_DELAY_MS] and by a service created inside that window.
   */
  @Volatile
  var isStopping: Boolean = false
    private set

  fun setRunning(running: Boolean) {
    isRunning = running
  }

  fun setStopping(stopping: Boolean) {
    isStopping = stopping
  }
}
