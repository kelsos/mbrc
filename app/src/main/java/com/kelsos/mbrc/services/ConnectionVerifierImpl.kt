package com.kelsos.mbrc.services

import com.fasterxml.jackson.databind.ObjectMapper
import com.kelsos.mbrc.constants.Protocol
import com.kelsos.mbrc.data.SocketMessage
import com.kelsos.mbrc.di.modules.AppDispatchers
import com.kelsos.mbrc.networking.RequestManager
import kotlinx.coroutines.withContext
import javax.inject.Inject

class ConnectionVerifierImpl
@Inject constructor(
  private val mapper: ObjectMapper,
  private val requestManager: RequestManager,
  private val dispatchers: AppDispatchers
) : ConnectionVerifier {

  private fun getMessage(response: String) =
    mapper.readValue(response, SocketMessage::class.java)

  override suspend fun verify(): Boolean = withContext(dispatchers.io) {
    try {
      val connection = requestManager.openConnection(false)
      val response = requestManager.request(
        connection,
        SocketMessage.create(Protocol.VerifyConnection)
      )
      connection.close()
      val message = getMessage(response)

      if (Protocol.VerifyConnection == message.context) {
        return@withContext true
      }
    } catch (e: Exception) {
      return@withContext false
    }

    throw NoValidPluginConnection()
  }


  class NoValidPluginConnection : Exception()
}
