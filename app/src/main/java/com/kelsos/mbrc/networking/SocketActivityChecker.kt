package com.kelsos.mbrc.networking

import com.kelsos.mbrc.common.utilities.AppCoroutineDispatchers
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import timber.log.Timber

typealias Listener = () -> Unit

class SocketActivityChecker(dispatchers: AppCoroutineDispatchers) {
  private var deferred: Deferred<Unit>? = null
  private var listener: Listener? = null
  private val job = SupervisorJob()
  private val scope = CoroutineScope(job + dispatchers.network)

  @Volatile
  private var isRunning = false

  @Volatile
  private var consecutiveTimeouts = 0

  fun start() {
    if (isRunning) {
      Timber.v("Activity checker already running")
      return
    }

    isRunning = true
    consecutiveTimeouts = 0
    scope.launch {
      Timber.v("Starting activity checker")
      schedule()
    }
  }

  private suspend fun schedule() {
    cancel()
    if (!isRunning) return

    deferred =
      scope.async {
        delay(DELAY_MS)
        if (!isRunning) return@async

        consecutiveTimeouts++
        Timber.v("Ping timeout #$consecutiveTimeouts after %d ms", DELAY_MS)

        val result = runCatching {
          listener?.invoke()
        }

        if (result.isFailure) {
          Timber.e(result.exceptionOrNull(), "calling the onTimeout method failed")
        }

        // Reset consecutive timeout count after successful timeout handling
        if (result.isSuccess) {
          consecutiveTimeouts = 0
        }
      }
  }

  private suspend fun cancel() {
    deferred?.run {
      if (isActive) {
        cancelAndJoin()
      }
    }
  }

  fun stop() {
    if (!isRunning) {
      Timber.v("Activity checker already stopped")
      return
    }

    Timber.v("Stopping activity checker")
    isRunning = false
    consecutiveTimeouts = 0
    scope.launch { cancel() }
  }

  fun ping() {
    if (!isRunning) {
      Timber.v("Received ping but activity checker is not running")
      return
    }

    Timber.v("Received ping - resetting timeout")
    consecutiveTimeouts = 0 // Reset timeout count on successful ping
    scope.launch {
      if (isRunning) {
        schedule()
      }
    }
  }

  fun setPingTimeoutListener(listener: Listener?) {
    this.listener = listener
  }

  companion object {
    private const val DELAY_MS = 40_000L
    private const val MAX_CONSECUTIVE_TIMEOUTS = 3
  }

  fun getTimeoutCount(): Int = consecutiveTimeouts

  fun isHealthy(): Boolean = isRunning && consecutiveTimeouts < MAX_CONSECUTIVE_TIMEOUTS
}
