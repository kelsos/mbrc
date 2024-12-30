package com.kelsos.mbrc.utilities

import rx.Completable
import rx.Subscription
import timber.log.Timber
import java.util.concurrent.TimeUnit
import javax.inject.Singleton

@Singleton
class SocketActivityChecker {
  private var subscription: Subscription? = null
  private var pingTimeoutListener: PingTimeoutListener? = null

  fun start() {
    Timber.v("Starting activity checker")
    subscription = subscribe
  }

  private val subscribe: Subscription
    get() = Completable.timer(DELAY.toLong(), TimeUnit.SECONDS).subscribe({
      Timber.v("Ping was more than %d seconds ago", DELAY)
      pingTimeoutListener?.onTimeout()
    }) { Timber.v("Subscription failed") }

  fun stop() {
    Timber.v("Stopping activity checker")
    unsubscribe()
  }

  fun ping() {
    Timber.v("Received ping")
    unsubscribe()
    subscription = subscribe
  }

  private fun unsubscribe() {
    if (subscription != null && !subscription!!.isUnsubscribed) {
      subscription!!.unsubscribe()
    }
  }

  fun setPingTimeoutListener(pingTimeoutListener: PingTimeoutListener?) {
    this.pingTimeoutListener = pingTimeoutListener
  }

  interface PingTimeoutListener {
    fun onTimeout()
  }

  companion object {
    private const val DELAY = 40
  }
}
