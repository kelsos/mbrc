package com.kelsos.mbrc.networking.discovery

import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.wifi.WifiManager
import com.kelsos.mbrc.networking.connections.toConnection
import com.kelsos.mbrc.networking.protocol.Protocol
import com.squareup.moshi.Moshi
import okio.buffer
import okio.source
import timber.log.Timber
import java.io.IOException
import java.net.DatagramPacket
import java.net.Inet4Address
import java.net.InetAddress
import java.net.MulticastSocket
import java.net.NetworkInterface
import java.net.SocketException
import java.net.SocketTimeoutException

fun interface RemoteServiceDiscovery {
  suspend fun discover(): DiscoveryStop
}

class RemoteServiceDiscoveryImpl(
  private val manager: WifiManager,
  private val connectivityManager: ConnectivityManager,
  moshi: Moshi,
) : RemoteServiceDiscovery {
  private val adapter = moshi.adapter(DiscoveryMessage::class.java)

  private suspend fun <T> useMulticastLock(
    lock: WifiManager.MulticastLock,
    block: suspend () -> T,
  ): T {
    lock.setReferenceCounted(true)
    lock.acquire()
    try {
      return block()
    } finally {
      if (lock.isHeld) {
        lock.release()
      }
    }
  }

  private suspend fun <T> useMulticastSocket(block: suspend (socket: MulticastSocket) -> T): T {
    val group = InetAddress.getByName(DISCOVERY_ADDRESS)
    var socket: MulticastSocket? = null
    try {
      socket =
        MulticastSocket(MULTICAST_PORT).apply {
          soTimeout = RESPONSE_TIMEOUT
          joinGroup(group)
        }

      val data =
        adapter
          .toJson(
            DiscoveryMessage(
              context = Protocol.DISCOVERY,
              address = requireNotNull(getWifiAddress()),
            ),
          ).toByteArray()
      socket.send(DatagramPacket(data, data.size, group, MULTICAST_PORT))

      return block(socket)
    } catch (e: IOException) {
      Timber.v(e, "Failed to open multi cast socket")
      throw SocketCreationFailedException(e)
    } finally {
      try {
        socket?.leaveGroup(group)
        socket?.close()
      } catch (e: IOException) {
        Timber.v("While cleaning up the discovery %s", e.message)
      }
    }
  }

  private fun retryUntilNotifyContext(
    socket: MulticastSocket,
    maxAttempts: Int = 3,
    totalTimeoutMs: Long = 15000,
  ): DiscoveryMessage? {
    var attempts = 0
    val startTime = System.currentTimeMillis()
    val responses = mutableListOf<DiscoveryMessage>()

    while (attempts < maxAttempts && System.currentTimeMillis() - startTime < totalTimeoutMs) {
      val message = getDiscoveryMessage(socket)
      if (message != null) {
        responses.add(message)
        if (message.context == NOTIFY) {
          Timber.v("Found notify message after %d attempts", attempts + 1)
          return message
        }
      }
      attempts++
      Timber.v("Discovery attempt: %s, responses so far: %d", attempts, responses.size)
    }

    return responses.firstOrNull { it.context == NOTIFY }
  }

  override suspend fun discover(): DiscoveryStop {
    if (!isWifiConnected()) {
      return DiscoveryStop.NoWifi
    }

    return try {
      waitForServiceNotification()
    } catch (e: IOException) {
      Timber.e(e, "discovery failed")
      DiscoveryStop.NotFound
    }
  }

  private suspend fun waitForServiceNotification(): DiscoveryStop {
    return useMulticastLock(manager.createMulticastLock("locked")) {
      useMulticastSocket { socket ->
        val message = retryUntilNotifyContext(socket)

        return@useMulticastSocket if (message?.context == NOTIFY) {
          DiscoveryStop.Complete(message.toConnection())
        } else {
          DiscoveryStop.NotFound
        }
      }
    }
  }

  private fun getDiscoveryMessage(socket: MulticastSocket): DiscoveryMessage? =
    try {
      socket.discoveryMessage()
    } catch (_: SocketTimeoutException) {
      null
    } catch (e: IOException) {
      Timber.e(e, "Failed to get discovery message")
      null
    }

  private fun getWifiAddress(): String? {
    if (!isWifiConnected()) {
      return null
    }

    return try {
      findIpV4Address()
    } catch (e: SocketException) {
      Timber.e(e, "Failed to get wifi address")
      null
    }
  }

  private fun findIpV4Address(): String? {
    val interfaces = NetworkInterface.getNetworkInterfaces()
    for (networkInterface in interfaces) {
      if (!networkInterface.isUp || networkInterface.isLoopback) continue
      for (address in networkInterface.inetAddresses) {
        if (address !is Inet4Address || address.isLoopbackAddress) continue
        return address.hostAddress
      }
    }
    return null
  }

  private fun isWifiConnected(): Boolean {
    val network = connectivityManager.activeNetwork
    val capabilities = network?.let { connectivityManager.getNetworkCapabilities(it) }
    return capabilities?.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) == true
  }

  private fun MulticastSocket.discoveryMessage(): DiscoveryMessage? {
    val buffer = ByteArray(BUFFER_SIZE)
    val packet = DatagramPacket(buffer, buffer.size)
    receive(packet)

    if (packet.length >= BUFFER_SIZE) {
      Timber.w("Discovery message may have been truncated (received %d bytes)", packet.length)
    }

    val byteSource = buffer.inputStream(0, packet.length).source().buffer()
    val discoveryMessage = adapter.fromJson(byteSource)
    Timber.v("Discovery parsed -> $discoveryMessage (from %s:%d)", packet.address?.hostAddress, packet.port)
    if (discoveryMessage != null && discoveryMessage.address.isEmpty()) {
      Timber.w("Received discovery message with empty address, using sender address")
      return discoveryMessage.copy(address = packet.address?.hostAddress.orEmpty())
    }
    return discoveryMessage
  }

  private class SocketCreationFailedException(
    cause: Throwable,
  ) : IOException(cause)

  companion object {
    private const val BUFFER_SIZE = 1024
    private const val NOTIFY = "notify"
    private const val RESPONSE_TIMEOUT = 2000
    private const val MULTICAST_PORT = 45345
    private const val DISCOVERY_ADDRESS = "239.1.5.10"
  }
}
