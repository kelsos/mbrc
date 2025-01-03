package com.kelsos.mbrc.networking.client

import com.kelsos.mbrc.common.utilities.AppCoroutineDispatchers
import com.kelsos.mbrc.data.DeserializationAdapter
import com.kelsos.mbrc.networking.RequestManager
import com.kelsos.mbrc.networking.protocol.Protocol
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.io.IOException

fun interface ConnectivityVerifier {
  suspend fun verify(): Boolean
}

class ConnectivityVerifierImpl(
  private val deserializationAdapter: DeserializationAdapter,
  private val requestManager: RequestManager,
  private val dispatchers: AppCoroutineDispatchers,
) : ConnectivityVerifier {
  private fun getMessage(response: String) =
    deserializationAdapter.objectify(
      response,
      SocketMessage::class,
    )

  override suspend fun verify(): Boolean =
    withContext(dispatchers.io) {
      try {
        val connection = requestManager.openConnection(false)
        val response =
          requestManager.request(
            connection,
            SocketMessage.create(Protocol.VerifyConnection),
          )
        connection.close()
        val message = getMessage(response)

        if (Protocol.VERIFY_CONNECTION == message.context) {
          return@withContext true
        }
      } catch (e: IOException) {
        Timber.e(e)
        return@withContext false
      }

      throw NoValidPluginConnection()
    }

  class NoValidPluginConnection : Exception()
}
