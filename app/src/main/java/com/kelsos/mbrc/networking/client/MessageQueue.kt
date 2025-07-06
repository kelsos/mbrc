package com.kelsos.mbrc.networking.client

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow

interface MessageQueue {
  suspend fun queue(message: SocketMessage)

  val messages: Flow<SocketMessage>
}

class MessageQueueImpl : MessageQueue {
  override val messages =
    MutableSharedFlow<SocketMessage>(
      extraBufferCapacity = EXTRA_BUFFER_CAPACITY
    )

  override suspend fun queue(message: SocketMessage) {
    messages.emit(message)
  }

  companion object {
    private const val EXTRA_BUFFER_CAPACITY = 10
  }
}
