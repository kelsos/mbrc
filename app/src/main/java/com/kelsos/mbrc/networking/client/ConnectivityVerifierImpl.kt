package com.kelsos.mbrc.networking.client

import com.fasterxml.jackson.databind.ObjectMapper
import com.kelsos.mbrc.common.utilities.AppCoroutineDispatchers
import com.kelsos.mbrc.networking.RequestManager
import com.kelsos.mbrc.networking.protocol.Protocol
import kotlinx.coroutines.withContext

class ConnectivityVerifierImpl(
  private val mapper: ObjectMapper,
  private val requestManager: RequestManager,
  private val dispatchers: AppCoroutineDispatchers,
) : ConnectivityVerifier {
  private fun getMessage(response: String) = mapper.readValue(response, SocketMessage::class.java)

  override suspend fun verify(): Boolean =
    withContext(dispatchers.io) {
      try {
        val connection = requestManager.openConnection(false)
        val response =
          requestManager.request(
            connection,
            SocketMessage.create(Protocol.VERIFY_CONNECTION),
          )
        connection.close()
        val message = getMessage(response)

        if (Protocol.VERIFY_CONNECTION == message.context) {
          return@withContext true
        }
      } catch (e: Exception) {
        return@withContext false
      }

      throw NoValidPluginConnection()
    }

  class NoValidPluginConnection : Exception()
}
