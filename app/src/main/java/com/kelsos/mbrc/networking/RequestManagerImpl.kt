package com.kelsos.mbrc.networking

import com.kelsos.mbrc.common.utilities.AppDispatchers
import com.kelsos.mbrc.data.DeserializationAdapter
import com.kelsos.mbrc.data.SerializationAdapter
import com.kelsos.mbrc.networking.client.SocketMessage
import com.kelsos.mbrc.networking.client.player
import com.kelsos.mbrc.networking.client.protocol
import com.kelsos.mbrc.networking.connections.ConnectionRepository
import com.kelsos.mbrc.networking.connections.InetAddressMapper
import com.kelsos.mbrc.networking.protocol.Protocol
import com.kelsos.mbrc.networking.protocol.ProtocolPayload
import com.kelsos.mbrc.preferences.ClientInformationStore
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.io.IOException
import java.net.Socket
import java.nio.charset.Charset

class RequestManagerImpl(
  private val serializationAdapter: SerializationAdapter,
  private val deserializationAdapter: DeserializationAdapter,
  private val clientInformationStore: ClientInformationStore,
  private val repository: ConnectionRepository,
  private val dispatchers: AppDispatchers
) : RequestManager {

  override suspend fun openConnection(
    handshake: Boolean
  ): ActiveConnection = withContext(dispatchers.io) {
    val firstMessage = if (handshake) SocketMessage.player() else null
    val socket = connect(firstMessage)

    val inputStream = socket.getInputStream()
    val bufferedReader = inputStream.bufferedReader(Charset.defaultCharset())

    while (handshake) {
      val line = bufferedReader.readLine()
      if (line == null || line.isEmpty()) {
        break
      }

      val message = deserializationAdapter.objectify(line, SocketMessage::class)

      val context = Protocol.fromString(message.context)
      Timber.v("incoming context => $context")
      if (Protocol.Player == context) {
        val payload = getProtocolPayload()
        socket.send(SocketMessage.protocol(payload))
      } else if (Protocol.ProtocolTag == context) {
        Timber.v("socket handshake is complete")
        break
      }
    }

    return@withContext ActiveConnection(socket, bufferedReader, handshake)
  }

  private suspend fun getProtocolPayload(): ProtocolPayload {
    return ProtocolPayload(clientInformationStore.getClientId()).apply {
      noBroadcast = true
      protocolVersion = Protocol.ProtocolVersionNumber
    }
  }

  override suspend fun request(
    connection: ActiveConnection,
    message: SocketMessage
  ): String = withContext(dispatchers.io) {
    connection.send(message.getBytes())

    val readLine = connection.readLine()

    return@withContext if (readLine.isEmpty()) {
      connection.readLine()
    } else {
      readLine
    }
  }

  private suspend fun connect(firstMessage: SocketMessage?): Socket {
    val mapper = InetAddressMapper()
    val connectionSettings = checkNotNull(repository.getDefault())

    try {
      Timber.v("Preparing connection to $connectionSettings")
      val socketAddress = mapper.map(connectionSettings)
      Timber.v("Creating new socket")

      return Socket().apply {
        soTimeout = 20 * 1000
        connect(socketAddress)
        firstMessage?.let {
          send(it)
        }
      }
    } catch (e: IOException) {
      Timber.v(e, "failed to create socket")
      throw e
    }
  }

  private fun SocketMessage.getBytes(): ByteArray {
    return (serializationAdapter.stringify(this) + "\r\n").toByteArray()
  }

  private fun Socket.send(socketMessage: SocketMessage) {
    this.outputStream.write(socketMessage.getBytes())
  }
}
