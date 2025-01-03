package com.kelsos.mbrc.data

import com.kelsos.mbrc.networking.client.SocketMessage
import com.squareup.moshi.Moshi

fun interface SerializationAdapter {
  fun stringify(message: SocketMessage): String
}

class SerializationAdapterImpl(
  private val moshi: Moshi,
) : SerializationAdapter {
  override fun stringify(message: SocketMessage): String {
    val adapter = moshi.adapter(SocketMessage::class.java)
    return adapter.toJson(message)
  }
}
