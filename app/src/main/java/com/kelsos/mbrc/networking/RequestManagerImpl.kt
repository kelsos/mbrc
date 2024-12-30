package com.kelsos.mbrc.networking

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.kelsos.mbrc.constants.Protocol
import com.kelsos.mbrc.data.ProtocolPayload
import com.kelsos.mbrc.data.SocketMessage
import com.kelsos.mbrc.di.modules.AppDispatchers
import com.kelsos.mbrc.mappers.InetAddressMapper
import com.kelsos.mbrc.repository.ConnectionRepository
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.io.IOException
import java.net.Socket
import java.nio.charset.Charset
import javax.inject.Inject

class RequestManagerImpl
@Inject
constructor(
  private val mapper: ObjectMapper,
  private val repository: ConnectionRepository,
  private val dispatchers: AppDispatchers
) : RequestManager {

  override suspend fun openConnection(handshake: Boolean): ActiveConnection =
    withContext(dispatchers.io) {
       val firstMessage = if (handshake) SocketMessage.create(Protocol.Player, "Android") else null
       val socket = connect(firstMessage)

       val inputStream = socket.getInputStream()
       val bufferedReader = inputStream.bufferedReader(Charset.defaultCharset())

       while (handshake) {
         val line = bufferedReader.readLine()
         if (line.isNullOrEmpty()) {
           break
         }

         val message = mapper.readValue<SocketMessage>(line)


         val context = message.context
         Timber.v("incoming context => $context")
         if (Protocol.Player == context) {
           val payload = getProtocolPayload()
           socket.send(SocketMessage.create(Protocol.ProtocolTag, payload))
         } else if (Protocol.ProtocolTag == context) {
           Timber.v("socket handshake complete")
           break
         }
       }


       return@withContext ActiveConnection(socket, bufferedReader, handshake)
     }

  private fun getProtocolPayload(): ProtocolPayload {
    return ProtocolPayload().apply {
      noBroadcast = true
      protocolVersion = Protocol.ProtocolVersionNumber
    }
  }

  override suspend fun request(connection: ActiveConnection, message: SocketMessage): String =
    withContext(dispatchers.io) {
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
      Timber.v("failed to create socket")
      throw e
    }
  }

  private fun SocketMessage.getBytes(): ByteArray {
    return (mapper.writeValueAsString(this) + "\r\n").toByteArray()
  }


  private fun Socket.send(socketMessage: SocketMessage) {
    this.outputStream.write(socketMessage.getBytes())
  }


}

