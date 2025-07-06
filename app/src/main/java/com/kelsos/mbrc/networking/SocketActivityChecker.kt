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

  fun start() {
    scope.launch {
      Timber.v("Starting activity checker")
      schedule()
    }
  }

  private suspend fun schedule() {
    cancel()
    deferred =
      scope.async {
        delay(DELAY_MS)
        Timber.v("Ping was more than %d seconds ago", DELAY_MS)
        val result = runCatching { listener?.invoke() }
        if (result.isFailure) {
          Timber.e(result.exceptionOrNull(), "calling the onTimeout method failed")
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
    Timber.v("Stopping activity checker")
    scope.launch { cancel() }
  }

  fun ping() {
    Timber.v("Received ping")
    scope.launch { schedule() }
  }

  fun setPingTimeoutListener(listener: Listener?) {
    this.listener = listener
  }

  companion object {
    private const val DELAY_MS = 40_000L
  }
}
