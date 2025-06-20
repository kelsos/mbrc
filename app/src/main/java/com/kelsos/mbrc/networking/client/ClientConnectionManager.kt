package com.kelsos.mbrc.networking.client

import com.kelsos.mbrc.common.state.ConnectionStatePublisher
import com.kelsos.mbrc.common.state.ConnectionStatus
import com.kelsos.mbrc.common.utilities.AppCoroutineDispatchers
import com.kelsos.mbrc.common.utilities.ScopeBase
import com.kelsos.mbrc.features.settings.ConnectionRepository
import com.kelsos.mbrc.networking.SocketActivityChecker
import com.kelsos.mbrc.networking.connections.toSocketAddress
import com.kelsos.mbrc.networking.discovery.DiscoveryStop
import com.squareup.moshi.Moshi
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import okio.buffer
import okio.sink
import okio.source
import timber.log.Timber
import java.io.IOException
import java.net.Socket
import java.net.SocketAddress
import java.net.SocketException
import java.net.SocketTimeoutException
import kotlin.math.pow

interface ClientConnectionManager {
  fun start()

  fun stop()
}

sealed class NetworkError : Exception() {
  data class ConnectionTimeout(
    override val cause: Throwable?,
  ) : NetworkError()

  data class ConnectionRefused(
    override val cause: Throwable?,
  ) : NetworkError()

  data class SocketError(
    override val cause: Throwable,
  ) : NetworkError()

  data class UnknownError(
    override val cause: Throwable,
  ) : NetworkError()
}

data class ConnectionConfig(
  val maxRetries: Int = 3,
  val initialDelay: Long = 1000L,
  val maxDelay: Long = 30000L,
  val backoffMultiplier: Double = 2.0,
)

class ClientConnectionManagerImpl(
  private val activityChecker: SocketActivityChecker,
  private val messageHandler: MessageHandler,
  private val moshi: Moshi,
  private val connectionRepository: ConnectionRepository,
  private val connectionState: ConnectionStatePublisher,
  private val dispatchers: AppCoroutineDispatchers,
  private val uiMessageQueue: UiMessageQueue,
) : ScopeBase(dispatchers.io),
  ClientConnectionManager {
  private var connection: Connection? = null
  private val connectionConfig = ConnectionConfig()

  override fun start() {
    stop()
    launch {
      delay(DELAY_MS)
      val currentStatus = connectionState.connection.firstOrNull()
      if (currentStatus != ConnectionStatus.Connected) {
        attemptConnection()
      }
    }
  }

  private suspend fun attemptConnection() {
    val connectionSettings = getConnectionSettings() ?: return
    Timber.v("Attempting connection on $connectionSettings")

    attemptConnectionWithRetry(connectionSettings.toSocketAddress())
  }

  private suspend fun attemptConnectionWithRetry(address: SocketAddress) {
    repeat(connectionConfig.maxRetries) { attempt ->
      if (attempt > 0) {
        val delayMs =
          minOf(
            connectionConfig.initialDelay * connectionConfig.backoffMultiplier.pow(attempt - 1).toLong(),
            connectionConfig.maxDelay,
          )
        Timber.v("Retrying connection in ${delayMs}ms (attempt ${attempt + 1}/${connectionConfig.maxRetries})")
        delay(delayMs)
      }

      val result = runCatching { Connection.connect(address) }
      result.fold(
        onSuccess = { socket ->
          Timber.v("Connection successful on attempt ${attempt + 1}")
          setupConnection(socket)
          return
        },
        onFailure = { exception ->
          val networkError = classifyNetworkError(exception)
          Timber.w("Connection attempt ${attempt + 1} failed: ${networkError::class.simpleName}")

          if (attempt == connectionConfig.maxRetries - 1) {
            Timber.e(exception, "All connection attempts failed")
            handleConnectionFailure(networkError)
            uiMessageQueue.messages.emit(UiMessage.ConnectionError.AllRetriesExhausted)
          }
        },
      )
    }
  }

  private fun classifyNetworkError(exception: Throwable): NetworkError =
    when (exception) {
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
                message ?: "Socket connection failed",
              )
          }
        }
        is NetworkError.UnknownError ->
          UiMessage.ConnectionError.UnknownConnectionError(
            networkError.cause.message ?: "Unknown connection error",
          )
      }

    uiMessageQueue.messages.emit(uiMessage)
  }

  private suspend fun getConnectionSettings() = connectionRepository.getDefault() ?: discoverConnection()

  private suspend fun discoverConnection() =
    connectionRepository.discover().let { discoveryStop ->
      when (discoveryStop) {
        is DiscoveryStop.Complete -> {
          Timber.v("Discovery detected ${discoveryStop.settings.address} will attempt to connect to it")
          discoveryStop.settings
        }
        else -> {
          Timber.v("Discovery did not complete, will not connect to any servers")
          null
        }
      }
    }

  private fun setupConnection(socket: Socket) {
    val connection =
      Connection(socket, moshi, dispatchers).also {
        this.connection = it
      }

    setupMessageFlows(connection)
    startConnectionWorker(connection)
    setupActivityChecker(connection)
  }

  private fun setupMessageFlows(connection: Connection) {
    launch {
      connection.messages.collect { message ->
        messageHandler.processIncoming(message)
      }
    }

    launch {
      messageHandler.processOutgoing { message ->
        connection.send(message).fold(
          onSuccess = { true },
          onFailure = { exception ->
            Timber.e(exception, "Send failed")
            if (!connection.isConnected) {
              connection.cleanup()
              stop()
            }
          },
        )
      }
    }
  }

  private fun startConnectionWorker(connection: Connection) {
    launch(dispatchers.io) {
      Timber.v("Socket connection is running")
      handleConnectionStatus(connection.isConnected)

      try {
        connection.listen()
      } catch (e: IOException) {
        Timber.e(e, "Connection worker failed due to IO error")
      } finally {
        handleConnectionStatus(connection.isConnected)
      }
    }
  }

  private fun setupActivityChecker(connection: Connection) {
    activityChecker.start()
    activityChecker.setPingTimeoutListener {
      Timber.v("Timeout received resetting socket")
      connection.cleanup()
      activityChecker.stop()
    }
  }

  private fun handleConnectionStatus(connected: Boolean) {
    launch {
      if (!connected) {
        activityChecker.stop()
        connectionState.updateConnection(ConnectionStatus.Offline)
      } else {
        connectionState.updateConnection(ConnectionStatus.Authenticating)
        messageHandler.startHandshake()
      }
    }
  }

  override fun stop() {
    connection?.cleanup()
    activityChecker.stop()
  }

  companion object {
    private const val DELAY_MS = 2000L
  }
}

class Connection(
  private val socket: Socket,
  moshi: Moshi,
  dispatchers: AppCoroutineDispatchers,
) {
  private val sink = socket.sink().buffer()
  private val source = socket.source().buffer()
  private val adapter = moshi.adapter(SocketMessage::class.java)
  private val job = SupervisorJob()
  private val scope = CoroutineScope(job + dispatchers.io)
  private val _messages = MutableSharedFlow<SocketMessage>()
  val messages: Flow<SocketMessage> get() = _messages

  @Volatile
  private var isCleanedUp = false

  val isConnected get() = !isCleanedUp && socket.isConnected && !socket.isClosed

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

  fun send(message: SocketMessage) =
    runCatching {
      if (!isConnected) {
        Timber.d("Socket was not connected: skipping $message")
        return@runCatching
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
          val message = checkNotNull(adapter.fromJson(reply))
          scope.launch {
            _messages.emit(message)
          }
        }

      if (result.isFailure) {
        val throwable = result.exceptionOrNull()
        Timber.e(throwable, "Failed processing $reply")
      }
    }
  }

  fun listen() {
    try {
      while (isConnected) {
        val rawMessage = source.readUtf8Line()
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

  companion object {
    private const val SO_TIMEOUT = 30_000
    private const val NEWLINE = "\r\n"

    fun connect(address: SocketAddress): Socket {
      val socket = Socket()
      socket.soTimeout = SO_TIMEOUT
      socket.connect(address)
      return socket
    }
  }
}
