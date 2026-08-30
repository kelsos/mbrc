package com.kelsos.mbrc.core.networking

import com.kelsos.mbrc.core.common.state.ConnectionStatePublisher
import com.kelsos.mbrc.core.common.state.ConnectionStatus
import com.kelsos.mbrc.core.common.utilities.coroutines.AppCoroutineDispatchers
import com.kelsos.mbrc.core.common.utilities.coroutines.ScopeBase
import com.kelsos.mbrc.core.networking.client.PendingCommandBuffer
import com.kelsos.mbrc.core.networking.client.SocketMessage
import com.kelsos.mbrc.core.networking.client.UiMessage
import com.kelsos.mbrc.core.networking.client.UiMessageQueue
import com.kelsos.mbrc.core.networking.discovery.DiscoveryStop
import com.squareup.moshi.Moshi
import java.io.IOException
import java.net.Socket
import java.net.SocketAddress
import java.net.SocketException
import java.net.SocketTimeoutException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import okio.buffer
import okio.sink
import okio.source
import timber.log.Timber

interface ClientConnectionManager {
  /**
   * Makes one connection attempt without waiting for it. Used by callers that have no coroutine to
   * suspend in, such as the service starting up.
   */
  fun start(cycleInfo: ConnectionCycleInfo? = null)

  /**
   * Makes one connection attempt and reports whether it reached [ConnectionStatus.Connected].
   *
   * Exactly one attempt: retrying is the retry driver's job, and having both layers retry meant
   * each restart cancelled the attempt already in flight.
   */
  suspend fun connect(cycleInfo: ConnectionCycleInfo? = null): Boolean

  fun stop()
}

/**
 * Raised when a message could not be written because the socket is no longer usable. Distinct from
 * a write that failed mid-flight: nothing reached the wire, so the command is safe to replay.
 */
class SocketNotConnectedException(message: SocketMessage) :
  IOException("Socket was not connected: $message")

sealed class NetworkError : Exception() {
  data class ConnectionTimeout(override val cause: Throwable?) : NetworkError()

  data class ConnectionRefused(override val cause: Throwable?) : NetworkError()

  data class SocketError(override val cause: Throwable) : NetworkError()

  data class UnknownError(override val cause: Throwable) : NetworkError()
}

data class ConnectionConfig(
  /**
   * How long a socket may sit in [ConnectionStatus.Authenticating] before the attempt is treated
   * as failed. The handshake is a round trip that a wedged server can leave unanswered forever,
   * and without a deadline the retry driver would wait on an attempt that never resolves.
   */
  val handshakeTimeoutMs: Long = 8000L,
  // Slightly less than ping timeout
  val readTimeoutMs: Long = 35000L,
  // Healthcheck every 20 seconds
  val healthCheckIntervalMs: Long = 20000L,
  // Safety ceiling for a single inbound line. Heap-relative; measured worst case on a 15k-track
  // library is ~244 KB per 800-item page, so this leaves ~100x headroom while preventing a
  // malformed/un-terminated frame from growing the read buffer until the heap is exhausted.
  val maxMessageBytes: Long = defaultMaxMessageBytes()
)

class ClientConnectionManagerImpl(
  private val activityChecker: SocketActivityChecker,
  private val messageHandler: MessageHandler,
  private val moshi: Moshi,
  private val connectionProvider: ConnectionProvider,
  private val connectionState: ConnectionStatePublisher,
  private val dispatchers: AppCoroutineDispatchers,
  private val uiMessageQueue: UiMessageQueue,
  private val pendingCommands: PendingCommandBuffer,
  private val localNetworkAccess: LocalNetworkAccess
) : ScopeBase(dispatchers.io),
  ClientConnectionManager {
  // Written when a connection is set up or torn down, read by the outgoing pump, which runs on a
  // different thread of the io pool.
  @Volatile
  private var connection: Connection? = null

  @Volatile
  private var connectionScope: CoroutineScope? = null
  private val connectionConfig = ConnectionConfig()
  private var currentCycleInfo: ConnectionCycleInfo? = null

  // Outlives individual connections, and stop(): see startOutgoingPump.
  private val pumpScope = CoroutineScope(SupervisorJob() + dispatchers.io)

  init {
    startOutgoingPump()
  }

  @Volatile
  private var isStopping = false

  // Track pending socket for cancellation during connect
  @Volatile
  private var pendingSocket: Socket? = null

  /**
   * Yields to an attempt that is already under way, unlike [connect], which always restarts.
   *
   * The service is asked to start asynchronously, so its own start can land after the drawer or
   * the retry driver has already opened a socket. Restarting here would tear that attempt down and
   * leave the caller waiting on a handshake for a connection that no longer exists.
   */
  override fun start(cycleInfo: ConnectionCycleInfo?) {
    if (attemptInFlight()) {
      Timber.v("A connection attempt is already in flight, not starting another")
      return
    }
    prepareForAttempt(cycleInfo) ?: return
    launch { runAttempt() }
  }

  private fun attemptInFlight(): Boolean = when (connectionState.connection.value) {
    is ConnectionStatus.Connecting, ConnectionStatus.Authenticating -> true
    else -> false
  }

  /**
   * Runs on [AppCoroutineDispatchers.io] whatever the caller's context is: the attempt blocks in
   * [Socket.connect], and both callers suspend on the main dispatcher.
   */
  override suspend fun connect(cycleInfo: ConnectionCycleInfo?): Boolean =
    withContext(dispatchers.io) {
      prepareForAttempt(cycleInfo)
        ?: return@withContext connectionState.connection.value == ConnectionStatus.Connected
      runAttempt()
    }

  /**
   * Tears down whatever came before and publishes [ConnectionStatus.Connecting], or returns null
   * when there is nothing to attempt.
   *
   * Refusing a missing permission here means it can never be presented as a connection attempt,
   * whichever caller asked. Being connected already is the other case worth refusing: an attempt
   * would drop a working session to rebuild the same one.
   */
  private fun prepareForAttempt(cycleInfo: ConnectionCycleInfo?): Unit? {
    if (!localNetworkAccess.isPermitted()) {
      Timber.d("Local network access is not permitted, refusing to connect")
      connectionState.updateConnection(ConnectionStatus.LocalNetworkDenied)
      return null
    }

    if (connectionState.connection.value == ConnectionStatus.Connected) {
      Timber.v("Already connected, ignoring start request")
      return null
    }

    tearDown(publishOffline = false)
    isStopping = false
    onStart()
    currentCycleInfo = cycleInfo
    connectionState.updateConnection(
      ConnectionStatus.Connecting(
        cycle = cycleInfo?.cycle,
        maxCycles = cycleInfo?.maxCycles ?: DEFAULT_MAX_CYCLES
      )
    )
    return Unit
  }

  private suspend fun runAttempt(): Boolean {
    val connectionSettings = getConnectionSettings()
    if (connectionSettings == null) {
      Timber.v("No connection settings available, going offline")
      connectionState.updateConnection(ConnectionStatus.Offline)
      notifyUser(UiMessage.ConnectionError.ServerNotFound)
      return false
    }
    Timber.v("Attempting connection on $connectionSettings")
    return attemptConnectionOnce(connectionSettings.toSocketAddress())
  }

  /**
   * One attempt, reporting whether the handshake completed.
   *
   * A socket that opens is not yet a usable connection: the handshake is a round trip a wedged
   * server can leave unanswered, so the attempt is only a success once the state reaches
   * [ConnectionStatus.Connected] within [ConnectionConfig.handshakeTimeoutMs].
   */
  private suspend fun attemptConnectionOnce(address: SocketAddress): Boolean {
    val result = runCatching { connectWithTracking(address) }

    result.onFailure { exception ->
      if (isStopping) {
        Timber.v("Connection attempt cancelled - stop requested")
        return false
      }
      val networkError = classifyNetworkError(exception)
      Timber.w("Connection attempt failed: ${networkError::class.simpleName}")
      handleConnectionFailure(networkError)
      return false
    }

    val socket = result.getOrThrow()
    if (isStopping) {
      Timber.v("Connection cancelled during connect - closing socket")
      runCatching { socket.close() }
      return false
    }

    setupConnection(socket)
    return awaitHandshake()
  }

  private suspend fun awaitHandshake(): Boolean {
    val connected = withTimeoutOrNull(connectionConfig.handshakeTimeoutMs) {
      connectionState.connection.first { it == ConnectionStatus.Connected }
    } != null

    if (!connected) {
      Timber.w("Handshake did not complete within ${connectionConfig.handshakeTimeoutMs}ms")
      teardownConnection()
      activityChecker.stop()
      connectionState.updateConnection(ConnectionStatus.Offline)
      notifyUser(UiMessage.ConnectionError.ConnectionTimeout)
    }
    return connected
  }

  private fun connectWithTracking(address: SocketAddress): Socket {
    val socket = Socket()
    pendingSocket = socket
    try {
      socket.soTimeout = Connection.SO_TIMEOUT
      socket.tcpNoDelay = true
      socket.keepAlive = true
      socket.connect(address, Connection.CONNECT_TIMEOUT)
      return socket
    } finally {
      pendingSocket = null
    }
  }

  private fun classifyNetworkError(exception: Throwable): NetworkError = when (exception) {
    is SocketTimeoutException -> NetworkError.ConnectionTimeout(exception)

    is SocketException ->
      if (exception.message?.contains("refused") == true) {
        NetworkError.ConnectionRefused(exception)
      } else {
        NetworkError.SocketError(exception)
      }

    is IOException -> NetworkError.SocketError(exception)

    else -> NetworkError.UnknownError(exception)
  }

  /**
   * Reports a failed attempt to the user, unless the retry driver is the one attempting: it
   * announces the whole sequence once when it gives up, and a message per cycle would queue five
   * snackbars across a single outage.
   */
  private suspend fun notifyUser(message: UiMessage) {
    if (currentCycleInfo == null) {
      uiMessageQueue.messages.emit(message)
    }
  }

  /**
   * A failed attempt always lands on [ConnectionStatus.Offline].
   *
   * The retry driver publishes its own [ConnectionStatus.Connecting] before each attempt, so
   * reporting the failure truthfully cannot be mistaken for giving up, and leaving the state on
   * Connecting instead would strand the UI there whenever the driver was not the caller.
   */
  private suspend fun handleConnectionFailure(networkError: NetworkError) {
    connectionState.updateConnection(ConnectionStatus.Offline)

    val uiMessage =
      when (networkError) {
        is NetworkError.ConnectionTimeout -> UiMessage.ConnectionError.ConnectionTimeout

        is NetworkError.ConnectionRefused -> UiMessage.ConnectionError.ConnectionRefused

        is NetworkError.SocketError -> {
          val message = networkError.cause.message
          when {
            message?.contains("Network is unreachable", ignoreCase = true) == true ->
              UiMessage.ConnectionError.NetworkUnavailable

            message?.contains("No route to host", ignoreCase = true) == true ->
              UiMessage.ConnectionError.ServerNotFound

            else ->
              UiMessage.ConnectionError.UnknownConnectionError(
                message ?: "Socket connection failed"
              )
          }
        }

        is NetworkError.UnknownError ->
          UiMessage.ConnectionError.UnknownConnectionError(
            networkError.cause.message ?: "Unknown connection error"
          )
      }

    notifyUser(uiMessage)
  }

  private suspend fun getConnectionSettings() =
    connectionProvider.getDefault() ?: discoverConnection()

  private suspend fun discoverConnection() = connectionProvider.discover().let { discoveryStop ->
    when (discoveryStop) {
      is DiscoveryStop.Complete -> {
        val host = discoveryStop.first
        Timber.v("Discovery detected ${host.address} will attempt to connect to it")
        host
      }

      else -> {
        Timber.v("Discovery did not complete, will not connect to any servers")
        null
      }
    }
  }

  private fun setupConnection(socket: Socket) {
    // Every coroutine below belongs to this socket. Without tearing the previous ones down first,
    // a reconnect that skips stop() (the ping timeout path) leaves the old outgoing collector
    // subscribed to the shared message queue, so each command is also handed to a dead connection.
    teardownConnection()

    val scope = CoroutineScope(coroutineContext + SupervisorJob(coroutineContext[Job]))
    connectionScope = scope

    val connection =
      Connection(socket, moshi, dispatchers, connectionConfig).also {
        this.connection = it
      }

    setupMessageFlows(scope, connection)
    startConnectionWorker(scope, connection)
    setupActivityChecker(connection)
    startHealthChecker(scope, connection)
  }

  private fun teardownConnection() {
    connectionScope?.let { scope ->
      Timber.v("Cancelling coroutines of the previous connection")
      scope.cancel()
    }
    connectionScope = null
    connection?.cleanup()
    connection = null
  }

  private fun setupMessageFlows(scope: CoroutineScope, connection: Connection) {
    scope.launch {
      connection.messages.collect { message ->
        messageHandler.processIncoming(message)
      }
    }
  }

  /**
   * Drains the outgoing queue for as long as the manager exists, rather than for the lifetime of a
   * single connection.
   *
   * The queue is a shared flow without replay, so a command emitted while nothing is collecting is
   * gone: a tap during a reconnect would never reach a socket and would never be buffered either.
   * Keeping a single collector alive across connections means every command is either written or
   * handed to [pendingCommands].
   */
  private fun startOutgoingPump() {
    pumpScope.launch {
      messageHandler.processOutgoing { message ->
        dispatchOutgoing(message)
      }
    }
  }

  private fun dispatchOutgoing(message: SocketMessage) {
    val current = connection
    if (current == null) {
      bufferForReplay(message)
      return
    }

    current.send(message).onFailure { exception ->
      Timber.e(exception, "Send failed")
      // Only a message that never reached the wire is safe to replay. A write that failed
      // mid-flight may have put part of the frame on the socket already, and replaying it would
      // hand the player a second copy of a command that is not idempotent.
      if (exception is SocketNotConnectedException) {
        bufferForReplay(message)
      }
      // A failed write means the socket is broken. Closing it lets the connection worker publish
      // Offline, which is what drives reconnection. Calling stop() here instead would also cancel
      // a reconnect that is already in flight.
      current.cleanup()
    }
  }

  /**
   * Holds [message] until a connection comes back, unless the manager is stopped: after an explicit
   * disconnect there is no session to replay into, and a command buffered then would fire against
   * whichever session the user opens next.
   */
  private fun bufferForReplay(message: SocketMessage) {
    if (isStopping) {
      Timber.d("Manager is stopped, discarding $message instead of buffering it")
      return
    }
    if (pendingCommands.stash(message)) {
      Timber.d("No connection available, buffering $message for replay")
    }
  }

  private fun startConnectionWorker(scope: CoroutineScope, connection: Connection) {
    scope.launch(dispatchers.io) {
      Timber.v("Socket connection is running")
      handleConnectionStatus(scope, connection.isConnected)

      try {
        connection.listen()
      } catch (e: IOException) {
        Timber.e(e, "Connection worker failed due to IO error")
      } finally {
        handleConnectionStatus(scope, connection.isConnected)
      }
    }
  }

  /**
   * Drops the connection and reports it, leaving the reconnect to the retry driver.
   *
   * Retrying here as well used to race the driver: two attempts ran against the same socket, each
   * restart cancelling the other's in flight.
   */
  private fun setupActivityChecker(connection: Connection) {
    activityChecker.start()
    activityChecker.setPingTimeoutListener {
      Timber.v("Ping timeout received - resetting socket")
      connection.cleanup()
      activityChecker.stop()
      launch { connectionState.updateConnection(ConnectionStatus.Offline) }
    }
  }

  private fun startHealthChecker(scope: CoroutineScope, connection: Connection) {
    scope.launch {
      while (connection.isConnected) {
        delay(connectionConfig.healthCheckIntervalMs)

        // Perform health check
        if (!connection.isConnected) {
          Timber.v("Health check failed - connection no longer healthy")
          connection.cleanup()
          break
        }

        // Additional check: try to get socket info to verify connection
        val healthCheck = runCatching {
          val (remoteAddress, isInputShutdown, isOutputShutdown) = connection.getSocketInfo()
          remoteAddress != null && !isInputShutdown && !isOutputShutdown
        }

        if (healthCheck.isFailure || healthCheck.getOrNull() == false) {
          Timber.v("Socket health check failed")
          connection.cleanup()
          break
        }
      }
    }
  }

  private fun handleConnectionStatus(scope: CoroutineScope, connected: Boolean) {
    scope.launch {
      if (!connected) {
        activityChecker.stop()
        connectionState.updateConnection(ConnectionStatus.Offline)
      } else {
        connectionState.updateConnection(ConnectionStatus.Authenticating)
        messageHandler.startHandshake()
      }
    }
  }

  override fun stop() = tearDown(publishOffline = true)

  /**
   * @param publishOffline false when an attempt is about to start. Offline is what drives the
   * reconnection loop, so announcing it on the way into a connection makes the state manager treat
   * a restart as a fresh connection loss and start a second retry loop alongside this attempt.
   */
  private fun tearDown(publishOffline: Boolean) {
    Timber.v("Stopping connection manager")
    isStopping = true
    currentCycleInfo = null

    // Cancel all coroutines (including connection attempts)
    onStop()

    // Close any pending socket that's in the middle of connecting
    pendingSocket?.let { socket ->
      Timber.v("Closing pending socket during stop")
      runCatching { socket.close() }
      pendingSocket = null
    }

    teardownConnection()
    activityChecker.stop()

    // Set state to Offline immediately, unless something more specific already explains why there
    // is no connection. Denied local network access is terminal, and a stop is part of how that
    // state is reached (the service is torn down), so overwriting it here would put the UI back to
    // "not connected" and hide the only thing the user can act on.
    if (publishOffline &&
      connectionState.connection.value !is ConnectionStatus.LocalNetworkDenied
    ) {
      connectionState.updateConnection(ConnectionStatus.Offline)
    }
  }

  companion object {
    /**
     * Denominator shown while connecting outside the retry loop, where there is no cycle to count.
     * Kept equal to the driver's cycle count so the UI does not change denominator mid-reconnect.
     */
    private const val DEFAULT_MAX_CYCLES = 5
  }
}

class Connection(
  private val socket: Socket,
  moshi: Moshi,
  dispatchers: AppCoroutineDispatchers,
  private val config: ConnectionConfig = ConnectionConfig()
) {
  private val sink = socket.sink().buffer()
  private val source = socket.source().buffer()
  private val adapter = moshi.adapter(SocketMessage::class.java)
  private val job = SupervisorJob()
  private val scope = CoroutineScope(job + dispatchers.io)
  private val _messages = MutableSharedFlow<SocketMessage>(
    extraBufferCapacity = MESSAGE_BUFFER_CAPACITY,
    onBufferOverflow = BufferOverflow.DROP_OLDEST
  )
  val messages: Flow<SocketMessage> get() = _messages

  @Volatile
  private var isCleanedUp = false

  val isConnected get() = !isCleanedUp &&
    socket.isConnected &&
    !socket.isClosed &&
    isSocketHealthy()

  fun isSocketHealthy(): Boolean = runCatching {
    // More robust socket health check
    !socket.isInputShutdown && !socket.isOutputShutdown && socket.remoteSocketAddress != null
  }.getOrElse { false }

  // Expose socket for health checks
  internal fun getSocketInfo() = Triple(
    socket.remoteSocketAddress,
    socket.isInputShutdown,
    socket.isOutputShutdown
  )

  fun cleanup() {
    if (isCleanedUp) return
    isCleanedUp = true

    job.cancel()

    runCatching {
      if (sink.isOpen) {
        sink.flush()
        sink.close()
      }
    }.onFailure { Timber.w(it, "Failed to close sink") }

    runCatching {
      if (source.isOpen) {
        source.close()
      }
    }.onFailure { Timber.w(it, "Failed to close source") }

    runCatching {
      if (!socket.isClosed) {
        socket.close()
      }
    }.onFailure { Timber.w(it, "Failed to close socket") }
  }

  /**
   * Writes [message] to the socket. Failure to write is reported as [Result.failure] so the caller
   * can tear the connection down and replay the command: an unwritten message must never look like
   * a delivered one.
   */
  fun send(message: SocketMessage): Result<Unit> = runCatching {
    if (!isConnected) {
      throw SocketNotConnectedException(message)
    }
    val address = socket.remoteSocketAddress
    Timber.v("Sending to mbrc:/$address (connected: $isConnected)::$message")
    adapter.toJson(sink, message)
    sink.writeUtf8(NEWLINE)
    sink.flush()
  }

  private fun emitMessages(rawMessage: String) {
    val replies =
      rawMessage
        .split("\r\n".toRegex())
        .dropLastWhile(String::isEmpty)

    for (reply in replies) {
      val result =
        runCatching {
          if (reply.isBlank()) {
            Timber.v("Skipping blank message")
            return@runCatching
          }

          val message = adapter.fromJson(reply)
          if (message == null) {
            Timber.w("Received null message from: $reply")
            return@runCatching
          }

          _messages.tryEmit(message)
        }

      if (result.isFailure) {
        val throwable = result.exceptionOrNull()
        Timber.e(throwable, "Failed processing message: $reply")
        // If we consistently fail to parse messages, the connection might be corrupted
        messageParseFailureCount++
        if (messageParseFailureCount >= MAX_PARSE_FAILURES) {
          Timber.w(
            "Too many message parse failures ($messageParseFailureCount), treating as connection failure"
          )
          throw IOException("Message parsing consistently failing - connection corrupted")
        }
      } else {
        // Reset failure count on successful parse
        messageParseFailureCount = 0
      }
    }
  }

  @Volatile
  private var messageParseFailureCount = 0

  fun listen() {
    try {
      while (isConnected) {
        val rawMessage = readWithTimeout()
        if (rawMessage == null) {
          Timber.d("Connection closed by remote")
          break
        }

        if (rawMessage.isNotEmpty()) {
          emitMessages(rawMessage)
        }
      }
    } catch (e: IOException) {
      if (!isCleanedUp) {
        Timber.e(e, "Listener terminated due to IO error")
      }
    } finally {
      cleanup()
    }
  }

  private fun readWithTimeout(): String? {
    // Set read timeout to detect unresponsive connections
    val originalTimeout = socket.soTimeout
    return try {
      socket.soTimeout = config.readTimeoutMs.toInt()
      readBoundedLine()
    } catch (e: SocketTimeoutException) {
      Timber.w("Read timeout after ${config.readTimeoutMs}ms - connection may be unresponsive")
      throw IOException("Connection read timeout", e)
    } finally {
      socket.soTimeout = originalTimeout
    }
  }

  /**
   * Reads a single `\n`-terminated line, refusing to buffer more than [ConnectionConfig.maxMessageBytes]
   * bytes. A peer that never sends a terminator would otherwise grow the okio buffer until the heap
   * is exhausted (the OOM we are guarding against). Returns null on a clean remote close.
   */
  private fun readBoundedLine(): String? {
    val limit = config.maxMessageBytes
    val newlineIndex = source.indexOf(LINE_FEED, 0, limit)
    if (newlineIndex != -1L) {
      val line = source.readUtf8(newlineIndex)
      source.skip(1) // consume the '\n'
      return line.removeSuffix("\r")
    }
    // No terminator within the cap: either the stream ended, or the peer is sending an
    // oversized/garbage frame that would grow the buffer without bound.
    val buffered = source.buffer.size
    if (buffered >= limit) {
      throw IOException(
        "Inbound message exceeded $limit bytes without a line terminator; connection corrupted"
      )
    }
    return if (buffered == 0L) null else source.readUtf8().removeSuffix("\r")
  }

  companion object {
    internal const val SO_TIMEOUT = 30_000

    /**
     * On a local network a TCP connect either completes in milliseconds or the host is not
     * reachable, so this is generous rather than tight. It is public because the retry driver
     * sizes its budget against it.
     */
    const val CONNECT_TIMEOUT = 8_000
    private const val NEWLINE = "\r\n"
    private const val LINE_FEED = '\n'.code.toByte()
    private const val MAX_PARSE_FAILURES = 5
    private const val MESSAGE_BUFFER_CAPACITY = 128

    fun connect(address: SocketAddress): Socket {
      val socket = Socket()
      socket.soTimeout = SO_TIMEOUT
      socket.tcpNoDelay = true // Reduce latency for ping/pong
      socket.keepAlive = true // Enable TCP keep-alive
      socket.connect(address, CONNECT_TIMEOUT)
      return socket
    }
  }
}
