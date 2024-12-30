package com.kelsos.mbrc.networking

import timber.log.Timber
import java.io.BufferedReader
import java.io.IOException
import java.net.Socket

class ActiveConnection(
  private val socket: Socket,
  private val bufferedReader: BufferedReader,
  val handshaked: Boolean,
) {
  fun send(bytes: ByteArray) {
    socket.getOutputStream().write(bytes)
  }

  fun readLine(): String = bufferedReader.readLine()

  fun close() {
    socket.cleanup()
  }

  private fun Socket.cleanup() {
    Timber.v("Cleaning auxiliary socket")
    if (!this.isClosed) {
      try {
        this.close()
      } catch (ex: IOException) {
        Timber.v(ex, "Failed to clause the auxiliary socket")
      }
    }
  }
}
