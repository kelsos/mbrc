package com.kelsos.mbrc.mock

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.net.ServerSocket

class MockServer(
  private val port: Int,
  private val state: MockState
) {
  private var running = false

  fun start() {
    running = true
    val serverSocket = ServerSocket(port)
    println("[TCP] Server listening on port $port")

    while (running) {
      try {
        val clientSocket = serverSocket.accept()
        println("[TCP] New connection from ${clientSocket.inetAddress}")

        CoroutineScope(Dispatchers.IO).launch {
          ClientHandler(clientSocket, state).handle()
        }
      } catch (e: Exception) {
        if (running) {
          println("[TCP] Error accepting connection: ${e.message}")
        }
      }
    }

    serverSocket.close()
  }

  fun stop() {
    running = false
  }
}
