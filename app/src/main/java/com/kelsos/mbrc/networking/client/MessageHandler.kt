package com.kelsos.mbrc.networking.client

import com.kelsos.mbrc.common.state.ConnectionStatePublisher
import com.kelsos.mbrc.common.state.ConnectionStatus
import com.kelsos.mbrc.common.utilities.AppCoroutineDispatchers
import com.kelsos.mbrc.features.library.LibrarySyncWorkHandler
import com.kelsos.mbrc.features.settings.ClientInformationStore
import com.kelsos.mbrc.networking.protocol.CommandFactory
import com.kelsos.mbrc.networking.protocol.MessageEvent
import com.kelsos.mbrc.networking.protocol.Protocol
import com.kelsos.mbrc.networking.protocol.ProtocolAction
import com.kelsos.mbrc.networking.protocol.ProtocolPayload
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.util.concurrent.ConcurrentHashMap

interface MessageHandler {
  suspend fun processIncoming(message: SocketMessage)

  suspend fun processOutgoing(listener: (message: SocketMessage) -> Unit)

  suspend fun startHandshake()
}

class MessageHandlerImpl(
  private val commandFactory: CommandFactory,
  private val messageQueue: MessageQueue,
  private val uiMessageQueue: UiMessageQueue,
  private val connectionState: ConnectionStatePublisher,
  private val clientInformationStore: ClientInformationStore,
  private val librarySyncWorkHandler: LibrarySyncWorkHandler,
  private val dispatchers: AppCoroutineDispatchers,
) : MessageHandler {
  private val commands: ConcurrentHashMap<Protocol, ProtocolAction> = ConcurrentHashMap()

  override suspend fun processIncoming(message: SocketMessage) {
    withContext(dispatchers.network) {
      if (!isValidMessage(message)) {
        Timber.w("Invalid message received: $message")
        return@withContext
      }

      val context = Protocol.Companion.fromString(message.context)
      Timber.v("received message with context -> ${message.context} :: ${message.data}")

      when (context) {
        Protocol.ClientNotAllowed -> clientNotAllowed()
        Protocol.CommandUnavailable ->
          uiMessageQueue.messages.emit(
            UiMessage.PartyModeCommandUnavailable,
          )

        Protocol.UnknownCommand -> Unit
        else -> handle(message, context)
      }
    }
  }

  private fun isValidMessage(message: SocketMessage): Boolean =
    message.context.isNotBlank() &&
      message.context.length <= MAX_CONTEXT_LENGTH &&
      message.data.toString().length <= MAX_DATA_LENGTH

  private suspend fun handle(
    message: SocketMessage,
    context: Protocol,
  ) {
    val data = message.data

    if (handshake(context, data)) {
      return
    }

    val status = connectionState.connection.firstOrNull() ?: ConnectionStatus.Offline
    if (status == ConnectionStatus.Connected) {
      execute(MessageEvent(context, data))
    }
  }

  private suspend fun handshake(
    context: Protocol,
    dataPayload: Any,
  ): Boolean =
    when (context) {
      Protocol.Player -> {
        sendProtocolPayload()
        true
      }

      Protocol.ProtocolTag -> {
        val protocolVersion = parseProtocolVersion(dataPayload)

        if (isVersionSupported(protocolVersion)) {
          execute(MessageEvent(Protocol.ProtocolTag, protocolVersion))
          connectionState.updateConnection(ConnectionStatus.Connected)
          Timber.v("Handshake complete with protocol version $protocolVersion, sending init message")
          messageQueue.queue(SocketMessage.create(Protocol.Init))

          Timber.v("Starting automatic library sync after successful connection")
          librarySyncWorkHandler.sync(auto = true)
          true
        } else {
          Timber.w("Unsupported protocol version: $protocolVersion")
          uiMessageQueue.messages.emit(UiMessage.ConnectionError.UnsupportedProtocolVersion)
          connectionState.updateConnection(ConnectionStatus.Offline)
          false
        }
      }

      else -> false
    }

  private fun parseProtocolVersion(dataPayload: Any): Int =
    try {
      val versionString = dataPayload.toString()
      if (versionString.isBlank()) {
        Timber.w("Empty protocol version, using default")
        MIN_SUPPORTED_VERSION
      } else {
        val version = versionString.toFloat().toInt()
        if (version < 1) {
          Timber.w("Invalid protocol version: $version, using minimum supported")
          MIN_SUPPORTED_VERSION
        } else {
          version
        }
      }
    } catch (e: NumberFormatException) {
      Timber.w(e, "Failed to parse protocol version: $dataPayload, using default")
      MIN_SUPPORTED_VERSION
    }

  private fun isVersionSupported(version: Int): Boolean = version in MIN_SUPPORTED_VERSION..Protocol.PROTOCOL_VERSION

  companion object {
    private const val MIN_SUPPORTED_VERSION = 2
    private const val MAX_CONTEXT_LENGTH = 100
    private const val MAX_DATA_LENGTH = 10_000
  }

  private suspend fun execute(event: MessageEvent) {
    get(event.type)
      .onSuccess { it.execute(event) }
      .onFailure { Timber.e(it, "Failed to execute command for $event") }
  }

  private fun get(context: Protocol) =
    runCatching {
      commands.computeIfAbsent(context) { protocol ->
        commandFactory.create(protocol)
      }
    }

  private suspend fun sendProtocolPayload() {
    val payload =
      ProtocolPayload(
        clientId = clientInformationStore.getClientId(),
        noBroadcast = false,
        protocolVersion = Protocol.PROTOCOL_VERSION,
      )
    messageQueue.queue(SocketMessage.create(Protocol.ProtocolTag, payload))
  }

  private suspend fun clientNotAllowed() {
    uiMessageQueue.messages.emit(UiMessage.NotAllowed)
    connectionState.updateConnection(ConnectionStatus.Offline)
  }

  override suspend fun processOutgoing(listener: (SocketMessage) -> Unit) {
    messageQueue.messages.collect { message ->
      withContext(dispatchers.network) {
        listener(message)
      }
    }
  }

  override suspend fun startHandshake() {
    Timber.v("Starting handshake")
    messageQueue.queue(SocketMessage.player())
  }
}
