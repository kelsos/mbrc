package com.kelsos.mbrc.networking.discovery

import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.wifi.WifiManager
import com.kelsos.mbrc.networking.connections.toConnection
import com.kelsos.mbrc.networking.protocol.Protocol
import com.squareup.moshi.Moshi
import kotlinx.coroutines.delay
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
          soTimeout = SO_TIMEOUT
          joinGroup(group)
          val data =
            adapter
              .toJson(
                DiscoveryMessage(
                  context = Protocol.DISCOVERY,
                  address = requireNotNull(getWifiAddress()),
                ),
              ).toByteArray()
          send(DatagramPacket(data, data.size, group, MULTICAST_PORT))
        }
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

  private suspend fun retryUntilNotifyContext(
    socket: MulticastSocket,
    maxAttempts: Int = 3,
    delayMillis: Long = 3000,
  ): DiscoveryMessage? {
    var attempts = 0
    var discoveryMessage: DiscoveryMessage? = null

    while (attempts < maxAttempts) {
      discoveryMessage = getDiscoveryMessage(socket)
      if (discoveryMessage?.context == NOTIFY) break
      delay(delayMillis)
      attempts++
      Timber.v("Discovery attempt: %s", attempts)
    }

    return discoveryMessage
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
    val byteSource = buffer.inputStream(0, packet.length).source().buffer()
    val discoveryMessage = adapter.fromJson(byteSource)
    Timber.v("Discovery parsed -> $discoveryMessage")
    return discoveryMessage
  }

  private class SocketCreationFailedException(
    cause: Throwable,
  ) : IOException(cause)

  companion object {
    private const val BUFFER_SIZE = 512
    private const val NOTIFY = "notify"
    private const val SO_TIMEOUT = 15 * 1000
    private const val MULTICAST_PORT = 45345
    private const val DISCOVERY_ADDRESS = "239.1.5.10"
  }
}
