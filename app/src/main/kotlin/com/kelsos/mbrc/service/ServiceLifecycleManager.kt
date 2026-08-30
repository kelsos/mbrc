package com.kelsos.mbrc.service

import android.app.Application
import android.content.Intent
import com.kelsos.mbrc.core.common.state.ConnectionStatePublisher
import com.kelsos.mbrc.core.common.state.ConnectionStatus
import com.kelsos.mbrc.core.common.utilities.coroutines.AppCoroutineDispatchers
import com.kelsos.mbrc.core.networking.ClientConnectionUseCase
import com.kelsos.mbrc.core.networking.Connection
import com.kelsos.mbrc.core.networking.ConnectionConfig
import com.kelsos.mbrc.core.networking.ConnectionCycleInfo
import com.kelsos.mbrc.core.networking.LocalNetworkAccess
import com.kelsos.mbrc.core.networking.client.UiMessage
import com.kelsos.mbrc.core.networking.client.UiMessageQueue
import com.kelsos.mbrc.core.networking.discovery.DiscoveryTiming
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import timber.log.Timber

/**
 * Manages the foreground service lifecycle based on connection state.
 *
 * When connection is lost, this manager will attempt to reconnect multiple times
 * before giving up and stopping the service. This provides resilience against
 * temporary network issues while still cleaning up when the server is truly unavailable.
 */
interface ServiceLifecycleManager {
  /**
   * Called when connection goes offline. Starts the reconnection loop
   * which will attempt to restore connection multiple times before
   * stopping the service.
   */
  fun onConnectionLost()

  /**
   * Called when connection is restored. Cancels any pending reconnection
   * attempts and resets the retry state.
   */
  fun onConnectionRestored()

  /**
   * Called when the user intentionally disconnects. Prevents the
   * reconnection loop from starting.
   */
  fun onIntentionalDisconnect()

  /**
   * Resets every latch to its cold-start value before a reconnect the user asked for.
   *
   * Giving up leaves the manager unable to reconnect for the life of the process: nothing
   * guarantees the reset that [onConnectionLost] and [onConnectionRestored] would perform.
   */
  fun onManualReconnect()

  /**
   * Check if the service is in the process of stopping due to connection failures.
   */
  val isStopPending: Boolean
}

class ServiceLifecycleManagerImpl(
  private val application: Application,
  private val connectionUseCase: ClientConnectionUseCase,
  private val connectionState: ConnectionStatePublisher,
  private val localNetworkAccess: LocalNetworkAccess,
  private val uiMessageQueue: UiMessageQueue,
  dispatchers: AppCoroutineDispatchers
) : ServiceLifecycleManager {
  private val scope = CoroutineScope(SupervisorJob() + dispatchers.main)
  private var reconnectionJob: Job? = null

  /**
   * Set the moment this manager decides to give up, before the Offline that announces it.
   *
   * That Offline comes straight back as a connection loss while the service is still tearing down,
   * so this is what tells the two apart. Without it a second full loop starts and the retry budget
   * doubles.
   */
  private val stopPending = AtomicBoolean(false)

  private val reconnectionCycle = AtomicInteger(0)
  private val isReconnecting = AtomicBoolean(false)
  private val intentionalDisconnect = AtomicBoolean(false)

  override val isStopPending: Boolean
    get() = stopPending.get()

  override fun onConnectionLost() {
    // Check if this was an intentional disconnect
    if (intentionalDisconnect.compareAndSet(true, false)) {
      Timber.v("Ignoring connection loss due to intentional disconnect")
      return
    }

    if (!ServiceState.isRunning || ServiceState.isStopping) {
      Timber.v("Service not running or already stopping, ignoring connection loss")
      return
    }

    if (stopPending.get()) {
      Timber.v("Already giving up on this connection, not starting another reconnection loop")
      return
    }

    // Retrying cannot recover from a missing permission, and presenting it as a reconnection
    // blames the network for something only the user can fix.
    if (!localNetworkAccess.isPermitted()) {
      Timber.d("Local network permission missing, not attempting to reconnect")
      connectionState.updateConnection(ConnectionStatus.LocalNetworkDenied)
      stopService(publishOffline = false)
      return
    }

    // Only start reconnection loop if not already running
    if (!isReconnecting.compareAndSet(false, true)) {
      Timber.v("Reconnection loop already running, ignoring duplicate connection loss")
      return
    }

    Timber.d("Connection lost, starting reconnection loop")
    reconnectionCycle.set(0)
    startReconnectionLoop()
  }

  override fun onIntentionalDisconnect() {
    Timber.d("Intentional disconnect requested")
    intentionalDisconnect.set(true)
    // Cancel any ongoing reconnection
    isReconnecting.set(false)
    reconnectionCycle.set(0)
    reconnectionJob?.cancel()
    reconnectionJob = null
    stopPending.set(false)
    // Stop the service and remove the notification
    stopService()
  }

  override fun onManualReconnect() {
    Timber.d("Manual reconnect requested, clearing reconnection state")
    intentionalDisconnect.set(false)
    isReconnecting.set(false)
    reconnectionCycle.set(0)
    stopPending.set(false)
    reconnectionJob?.cancel()
    reconnectionJob = null
  }

  /**
   * Clears the loop's flags without cancelling its job, because the job is what is running the
   * attempt that just succeeded. Cancelling it here killed the attempt mid-handshake; the loop
   * checks [isReconnecting] each pass and stops on its own.
   */
  override fun onConnectionRestored() {
    val wasReconnecting = isReconnecting.getAndSet(false)
    val cycle = reconnectionCycle.getAndSet(0)

    if (wasReconnecting) {
      Timber.d("Connection restored after $cycle reconnection cycle(s)")
    }

    stopPending.set(false)
  }

  private fun startReconnectionLoop() {
    reconnectionJob?.cancel()
    reconnectionJob = scope.launch {
      while (isReconnecting.get() && reconnectionCycle.get() < MAX_RECONNECTION_CYCLES) {
        val cycle = reconnectionCycle.incrementAndGet()
        val backoff = backoffFor(cycle)
        Timber.d("Reconnection cycle $cycle/$MAX_RECONNECTION_CYCLES, waiting ${backoff}ms")

        connectionState.updateConnection(
          ConnectionStatus.Connecting(cycle = cycle, maxCycles = MAX_RECONNECTION_CYCLES)
        )

        delay(backoff)

        if (!isReconnecting.get()) {
          Timber.d("Reconnection cancelled during delay")
          return@launch
        }

        Timber.d("Triggering reconnection attempt (cycle $cycle)")
        val connected = connectionUseCase.connect(
          cycleInfo = ConnectionCycleInfo(cycle = cycle, maxCycles = MAX_RECONNECTION_CYCLES)
        )

        if (connected) {
          Timber.d("Reconnected on cycle $cycle")
          return@launch
        }
      }

      if (isReconnecting.get()) {
        Timber.d("All $MAX_RECONNECTION_CYCLES reconnection cycles exhausted, stopping service")
        uiMessageQueue.messages.emit(UiMessage.ConnectionError.AllRetriesExhausted)
        stopService()
      }
    }
  }

  /**
   * Widens the gap between attempts, so a server that is briefly unreachable is found quickly
   * while one that is genuinely gone is not polled every few seconds for the whole budget.
   */
  private fun backoffFor(cycle: Int): Long = minOf(BACKOFF_STEP_MS * cycle, MAX_BACKOFF_MS)

  /**
   * @param publishOffline false when the caller has already published a more specific reason for
   * stopping, so it is not immediately overwritten by a generic Offline.
   */
  private fun stopService(publishOffline: Boolean = true) {
    if (stopPending.compareAndSet(false, true)) {
      Timber.d("Stopping service due to connection failure")
      isReconnecting.set(false)
      reconnectionCycle.set(0)
      if (publishOffline) {
        // Set state to Offline since all reconnection attempts have failed
        connectionState.updateConnection(ConnectionStatus.Offline)
      }
      application.stopService(Intent(application, RemoteService::class.java))
    }
  }

  companion object {
    /**
     * Number of attempts before the service gives up and stops.
     *
     * Each cycle is one attempt now, not a nested retry sequence, so the loop is what bounds the
     * whole effort. Derived from [RECONNECTION_BUDGET_MS] rather than chosen on its own.
     */
    const val MAX_RECONNECTION_CYCLES = 5

    /**
     * Ceiling on how long reconnection may run before the service gives up and stops. Holding a
     * foreground service open past this keeps a process alive the OS would otherwise reclaim.
     *
     * A test pins the worst case of the constants below to this budget.
     */
    const val RECONNECTION_BUDGET_MS = 180_000L

    /** Growth step of the wait before each attempt: 5s, 10s, 15s, and so on. */
    const val BACKOFF_STEP_MS = 5_000L

    /** Ceiling on a single backoff, so late attempts stay within a useful distance of each other. */
    const val MAX_BACKOFF_MS = 30_000L

    /**
     * Worst case duration of one attempt: a discovery scan that finds nothing, a connect that runs
     * to its timeout, then a handshake that never answers.
     *
     * Discovery only runs when no default connection is set, but the budget has to hold in that
     * case too.
     */
    val ATTEMPT_WORST_CASE_MS =
      DiscoveryTiming.SHIPPED.collectionWindowMs +
        Connection.CONNECT_TIMEOUT.toLong() +
        ConnectionConfig().handshakeTimeoutMs
  }
}
