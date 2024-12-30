package com.kelsos.mbrc.store

import androidx.datastore.core.CorruptionException
import androidx.datastore.core.Serializer
import com.google.protobuf.InvalidProtocolBufferException
import java.io.InputStream
import java.io.OutputStream

object StoreSerializer : Serializer<Store> {
  override suspend fun readFrom(input: InputStream): Store {
    try {
      return Store.parseFrom(input)
    } catch (exception: InvalidProtocolBufferException) {
      throw CorruptionException("Cannot read proto.", exception)
    }
  }

  override suspend fun writeTo(
    t: Store,
    output: OutputStream,
  ) = t.writeTo(output)

  override val defaultValue: Store
    get() = Store.getDefaultInstance()
}
