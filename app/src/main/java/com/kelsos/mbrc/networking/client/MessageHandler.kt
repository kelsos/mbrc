package com.kelsos.mbrc.networking.client

import com.kelsos.mbrc.common.state.ConnectionStatePublisher
import com.kelsos.mbrc.common.state.ConnectionStatus
import com.kelsos.mbrc.common.utilities.AppCoroutineDispatchers
import com.kelsos.mbrc.features.settings.ClientInformationStore
import com.kelsos.mbrc.networking.protocol.CommandFactory
import com.kelsos.mbrc.networking.protocol.MessageEvent
import com.kelsos.mbrc.networking.protocol.Protocol
import com.kelsos.mbrc.networking.protocol.ProtocolAction
import com.kelsos.mbrc.networking.protocol.ProtocolPayload
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.withContext
import timber.log.Timber

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
  private val dispatchers: AppCoroutineDispatchers,
) : MessageHandler {
  private val commands: MutableMap<Protocol, ProtocolAction> = HashMap()

  override suspend fun processIncoming(message: SocketMessage) {
    withContext(dispatchers.network) {
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
        val protocolVersion: Int =
          try {
            dataPayload.toString().toInt()
          } catch (ignore: NumberFormatException) {
            2
          }

        execute(MessageEvent(Protocol.ProtocolTag, protocolVersion))
        connectionState.updateConnection(ConnectionStatus.Connected)
        Timber.v("Handshake complete, sending init message")
        messageQueue.queue(SocketMessage.create(Protocol.Init))
        true
      }

      else -> false
    }

  private suspend fun execute(event: MessageEvent) {
    get(event.type)
      .onSuccess { it.execute(event) }
      .onFailure { Timber.e(it, "Failed to execute command for $event") }
  }

  private fun get(context: Protocol) =
    runCatching {
      val command = commands[context]
      if (command != null) {
        return@runCatching command
      }
      commandFactory.create(context).also { commandInstance ->
        commands[context] = commandInstance
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
