package com.kelsos.mbrc.networking

import com.kelsos.mbrc.common.data.Progress
import com.kelsos.mbrc.data.DeserializationAdapter
import com.kelsos.mbrc.networking.client.GenericSocketMessage
import com.kelsos.mbrc.networking.client.ResponseWithPayload
import com.kelsos.mbrc.networking.client.SocketMessage
import com.kelsos.mbrc.networking.protocol.Page
import com.kelsos.mbrc.networking.protocol.PageRange
import com.kelsos.mbrc.networking.protocol.Protocol
import com.squareup.moshi.Types
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import timber.log.Timber
import kotlin.reflect.KClass

class ApiBase(
  private val adapter: DeserializationAdapter,
  private val apiRequestManager: RequestManager,
) {
  suspend fun <T> getItem(
    request: Protocol,
    kClazz: KClass<T>,
    payload: Any = "",
  ): T where T : Any {
    val type = Types.newParameterizedType(GenericSocketMessage::class.java, kClazz.java)
    val connection = apiRequestManager.openConnection()
    val response = apiRequestManager.request(connection, SocketMessage.create(request, payload))
    connection.close()
    return adapter.objectify<GenericSocketMessage<T>>(response, type).data
  }

  fun <T : Any> getAllPages(
    request: Protocol,
    clazz: KClass<T>,
    progress: Progress?,
  ): Flow<List<T>> {
    val inner = Types.newParameterizedType(Page::class.java, clazz.java)
    val type = Types.newParameterizedType(GenericSocketMessage::class.java, inner)

    return flow {
      val start = now()
      val connection = apiRequestManager.openConnection()

      for (currentPage in 0..Int.MAX_VALUE) {
        val pageStart = now()
        val limit = LIMIT
        val offset = currentPage * limit
        val range = getPageRange(offset, limit)
        Timber.v("fetching $request offset $offset [$limit]")
        val message = SocketMessage.create(request, range ?: "")
        val response = apiRequestManager.request(connection, message)
        val socketMessage =
          adapter.objectify<GenericSocketMessage<Page<T>>>(
            response,
            type,
          )

        Timber.v("duration ${now() - pageStart} ms")
        val page = socketMessage.data

        progress?.invoke(page.offset + page.data.size, page.total)
        emit(page.data)
        if (page.offset + page.limit > page.total) {
          break
        }
      }
      connection.close()
      Timber.v("total duration ${System.currentTimeMillis() - start} ms")
    }
  }

  fun <T : Any, P : Any> getAll(
    request: Protocol,
    payload: List<P>,
    clazz: KClass<T>,
    progress: Progress?,
  ): Flow<ResponseWithPayload<P, T>> {
    val type = Types.newParameterizedType(GenericSocketMessage::class.java, clazz.java)

    return flow {
      val start = now()
      val connection = apiRequestManager.openConnection()
      var current = 0
      for (item in payload) {
        progress?.invoke(++current, payload.size)
        val entryStart = now()
        val message = SocketMessage.create(request, item)
        val response = apiRequestManager.request(connection, message)
        val socketMessage =
          adapter.objectify<GenericSocketMessage<T>>(
            response,
            type,
          )

        Timber.v("duration ${now() - entryStart} ms")
        emit(ResponseWithPayload(item, socketMessage.data))
      }
      connection.close()
      Timber.v("duration ${System.currentTimeMillis() - start} ms")
    }
  }

  private fun getPageRange(
    offset: Int,
    limit: Int,
  ): PageRange? =
    takeIf { limit > 0 }?.run {
      PageRange().apply {
        this.offset = offset
        this.limit = limit
      }
    }

  private fun now(): Long = System.currentTimeMillis()

  companion object {
    const val LIMIT = 800
  }
}
