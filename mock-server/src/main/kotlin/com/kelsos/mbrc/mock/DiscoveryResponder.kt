package com.kelsos.mbrc.mock

import com.kelsos.mbrc.mock.protocol.DiscoveryMessage
import com.kelsos.mbrc.mock.protocol.Protocol
import com.kelsos.mbrc.mock.protocol.json
import kotlinx.serialization.encodeToString
import java.net.DatagramPacket
import java.net.InetAddress
import java.net.MulticastSocket
import java.net.NetworkInterface

class DiscoveryResponder(
  private val tcpPort: Int
) {
  private var running = false
  private var socket: MulticastSocket? = null

  fun start() {
    running = true

    try {
      socket = MulticastSocket(Protocol.DISCOVERY_PORT)
      val group = InetAddress.getByName(Protocol.DISCOVERY_ADDRESS)

      // Join multicast group
      val networkInterface = findNetworkInterface()
      if (networkInterface != null) {
        socket?.joinGroup(java.net.InetSocketAddress(group, Protocol.DISCOVERY_PORT), networkInterface)
      } else {
        @Suppress("DEPRECATION")
        socket?.joinGroup(group)
      }

      println("[UDP] Discovery responder listening on ${Protocol.DISCOVERY_ADDRESS}:${Protocol.DISCOVERY_PORT}")

      val buffer = ByteArray(1024)
      while (running) {
        try {
          val packet = DatagramPacket(buffer, buffer.size)
          socket?.receive(packet)

          val message = String(packet.data, 0, packet.length)
          println("[UDP] Received: $message from ${packet.address}")

          val discovery = json.decodeFromString<DiscoveryMessage>(message)
          if (discovery.context == Protocol.DISCOVERY) {
            // Respond with our server info
            val localAddress = getLocalAddress() ?: packet.address.hostAddress
            val response = DiscoveryMessage(
              name = "MusicBee Mock",
              address = localAddress,
              port = tcpPort,
              context = Protocol.NOTIFY
            )

            val responseBytes = json.encodeToString(response).toByteArray()
            val responsePacket = DatagramPacket(
              responseBytes,
              responseBytes.size,
              packet.address,
              packet.port
            )
            socket?.send(responsePacket)
            println("[UDP] Sent discovery response to ${packet.address}:${packet.port}")
          }
        } catch (e: Exception) {
          if (running) {
            println("[UDP] Error: ${e.message}")
          }
        }
      }
    } catch (e: Exception) {
      println("[UDP] Failed to start discovery responder: ${e.message}")
    }
  }

  fun stop() {
    running = false
    socket?.close()
  }

  private fun findNetworkInterface(): NetworkInterface? {
    return NetworkInterface.getNetworkInterfaces()?.toList()
      ?.firstOrNull { ni ->
        !ni.isLoopback && ni.isUp && ni.inetAddresses.toList().any { it is java.net.Inet4Address }
      }
  }

  private fun getLocalAddress(): String? {
    return NetworkInterface.getNetworkInterfaces()?.toList()
      ?.filter { !it.isLoopback && it.isUp }
      ?.flatMap { it.inetAddresses.toList() }
      ?.firstOrNull { it is java.net.Inet4Address }
      ?.hostAddress
  }
}
