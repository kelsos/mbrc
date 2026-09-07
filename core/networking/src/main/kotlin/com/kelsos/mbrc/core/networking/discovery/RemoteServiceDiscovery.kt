package com.kelsos.mbrc.core.networking.discovery

import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.wifi.WifiManager
import com.kelsos.mbrc.core.networking.LocalNetworkAccess
import com.kelsos.mbrc.core.networking.protocol.Clock
import com.kelsos.mbrc.core.networking.protocol.base.Protocol
import com.squareup.moshi.Moshi
import java.io.IOException
import java.net.DatagramPacket
import java.net.Inet4Address
import java.net.InetAddress
import java.net.MulticastSocket
import java.net.SocketException
import java.net.SocketTimeoutException
import kotlinx.coroutines.yield
import okio.buffer
import okio.source
import timber.log.Timber

fun interface RemoteServiceDiscovery {
  suspend fun discover(): DiscoveryStop
}

/**
 * Binds a multicast socket to [port] and nothing else: the read timeout and the group to join are
 * the scan's decisions, so they stay in [RemoteServiceDiscoveryImpl] where a test can observe them.
 * This exists only because `MulticastSocket(port)` cannot be substituted otherwise.
 */
fun interface MulticastSockets {
  fun bind(port: Int): MulticastSocket
}

/** One local network interface, reduced to what picking an announce address needs. */
data class LocalInterface(
  val isUp: Boolean,
  val isLoopback: Boolean,
  val addresses: List<InetAddress>
)

/**
 * The host's network interfaces. A seam over `NetworkInterface.getNetworkInterfaces()` only; which
 * of them can carry the announcement is decided by [RemoteServiceDiscoveryImpl].
 */
fun interface NetworkInterfaces {
  @Throws(SocketException::class)
  fun all(): List<LocalInterface>
}

/**
 * How long a scan listens and how often it repeats itself. [SHIPPED] carries the tuned values; a
 * test substitutes shorter ones so a scan does not take the seconds those are tuned for.
 */
data class DiscoveryTiming(
  val responseTimeoutMs: Int,
  val collectionWindowMs: Long,
  val postFirstGatherMs: Long,
  val rebroadcastIntervalMs: Long
) {
  companion object {
    val SHIPPED = DiscoveryTiming(
      responseTimeoutMs = RESPONSE_TIMEOUT,
      collectionWindowMs = COLLECTION_WINDOW_MS,
      postFirstGatherMs = POST_FIRST_GATHER_MS,
      rebroadcastIntervalMs = REBROADCAST_INTERVAL_MS
    )

    private const val RESPONSE_TIMEOUT = 500

    // Maximum time the receiver listens for NOTIFY messages on a single
    // scan when nobody has answered yet. Long enough for slow first
    // responders without making the no-host case painful.
    private const val COLLECTION_WINDOW_MS = 4000L

    // Once the first NOTIFY has arrived, the loop keeps listening for
    // this much longer to collect siblings on multi-host LANs, then
    // returns. Tuned so single-host auto-connect doesn't pay the full
    // collection window and so peers responding to the same broadcast
    // (which generally answer within a few hundred ms of each other) all
    // make it into the result.
    private const val POST_FIRST_GATHER_MS = 1000L

    // How often the discovery packet is re-broadcast inside the collection
    // window so a single dropped multicast packet doesn't hide a host.
    private const val REBROADCAST_INTERVAL_MS = 1000L
  }
}

class RemoteServiceDiscoveryImpl(
  private val manager: WifiManager,
  private val connectivityManager: ConnectivityManager,
  private val localNetworkAccess: LocalNetworkAccess,
  moshi: Moshi,
  private val sockets: MulticastSockets,
  private val networkInterfaces: NetworkInterfaces,
  private val timing: DiscoveryTiming,
  private val clock: Clock
) : RemoteServiceDiscovery {
  private val adapter = moshi.adapter(DiscoveryMessage::class.java)

  private suspend fun <T> useMulticastLock(
    lock: WifiManager.MulticastLock,
    block: suspend () -> T
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

  private suspend fun <T> useMulticastSocket(
    address: String,
    block: suspend (socket: MulticastSocket) -> T
  ): T {
    val group = InetAddress.getByName(DISCOVERY_ADDRESS)
    var socket: MulticastSocket? = null
    try {
      socket = sockets.bind(MULTICAST_PORT).apply {
        soTimeout = timing.responseTimeoutMs
        joinGroup(group)
      }
      broadcastDiscovery(socket, group, address)

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

  /**
   * Collects every unique NOTIFY response that arrives within
   * [DiscoveryTiming.collectionWindowMs]. Re-broadcasts the discovery packet every
   * [DiscoveryTiming.rebroadcastIntervalMs] to combat UDP/multicast packet loss on
   * multi-host LANs (the original single-broadcast scan often missed
   * peers on the first try). De-duplicates by `(address, port)`.
   *
   * Returns early [DiscoveryTiming.postFirstGatherMs] after the first NOTIFY arrives
   * — siblings on the same LAN typically answer within ~1s of each
   * other, so the full 4s window is only paid when nobody answers.
   * This keeps the auto-connect cold-start path fast.
   *
   * Co-operative cancellation: [yield] is called between socket reads
   * so a cancelled caller (screen left, scope torn down) doesn't pin
   * the network thread for the rest of the window.
   */
  private suspend fun collectNotifyMessages(
    socket: MulticastSocket,
    address: String
  ): List<DiscoveryMessage> {
    val found = LinkedHashMap<Pair<String, Int>, DiscoveryMessage>()
    val startTime = clock.now()
    var firstResponseAt: Long? = null
    var lastBroadcast = startTime

    while (true) {
      yield()
      val now = clock.now()
      if (now - startTime >= timing.collectionWindowMs) break
      if (firstResponseAt != null && now - firstResponseAt >= timing.postFirstGatherMs) break

      val message = getDiscoveryMessage(socket)
      if (message != null && message.context == NOTIFY) {
        val isNew = found.putIfAbsent(message.address to message.port, message) == null
        if (isNew && firstResponseAt == null) {
          firstResponseAt = clock.now()
        }
      }
      if (clock.now() - lastBroadcast >= timing.rebroadcastIntervalMs) {
        rebroadcastDiscovery(socket, address)
        lastBroadcast = clock.now()
      }
    }

    Timber.v("Discovery window closed, %d unique host(s) found", found.size)
    return found.values.toList()
  }

  private fun rebroadcastDiscovery(socket: MulticastSocket, address: String) {
    try {
      broadcastDiscovery(socket, InetAddress.getByName(DISCOVERY_ADDRESS), address)
    } catch (e: IOException) {
      Timber.v(e, "Re-broadcast failed; will rely on already-collected responses")
    }
  }

  private fun broadcastDiscovery(socket: MulticastSocket, group: InetAddress, address: String) {
    val data = adapter.toJson(
      DiscoveryMessage(context = Protocol.DISCOVERY, address = address)
    ).toByteArray()
    socket.send(DatagramPacket(data, data.size, group, MULTICAST_PORT))
  }

  override suspend fun discover(): DiscoveryStop {
    // Multicast discovery needs the same permission the connection does, so without it a scan
    // would report "no hosts found" for a problem that has nothing to do with the network.
    if (!localNetworkAccess.isPermitted()) {
      return DiscoveryStop.LocalNetworkDenied
    }
    if (!isWifiConnected()) {
      return DiscoveryStop.NoWifi
    }

    // The plugin answers to the address in the packet, so a wifi connection whose interfaces carry
    // no IPv4 address has nothing to announce and no answer to expect.
    val address = announceAddress()
    if (address == null) {
      Timber.w("Connected to wifi but no IPv4 address was found, nothing to announce")
      return DiscoveryStop.NotFound
    }

    return try {
      waitForServiceNotification(address)
    } catch (e: IOException) {
      Timber.e(e, "discovery failed")
      DiscoveryStop.NotFound
    }
  }

  private suspend fun waitForServiceNotification(address: String): DiscoveryStop {
    return useMulticastLock(manager.createMulticastLock("locked")) {
      useMulticastSocket(address) { socket ->
        val messages = collectNotifyMessages(socket, address)

        return@useMulticastSocket if (messages.isNotEmpty()) {
          DiscoveryStop.Complete(messages.map { it.toConnection() })
        } else {
          DiscoveryStop.NotFound
        }
      }
    }
  }

  private fun getDiscoveryMessage(socket: MulticastSocket): DiscoveryMessage? = try {
    socket.discoveryMessage()
  } catch (_: SocketTimeoutException) {
    null
  } catch (e: IOException) {
    Timber.e(e, "Failed to get discovery message")
    null
  }

  /**
   * The first IPv4 address on an interface that is up and is not the loopback. A down interface has
   * no path to the plugin, and announcing a loopback or IPv6 address gives it nowhere to answer.
   */
  private fun announceAddress(): String? = try {
    networkInterfaces
      .all()
      .asSequence()
      .filter { it.isUp && !it.isLoopback }
      .flatMap { it.addresses }
      .filterIsInstance<Inet4Address>()
      .firstOrNull { !it.isLoopbackAddress }
      ?.hostAddress
  } catch (e: SocketException) {
    Timber.e(e, "Failed to get wifi address")
    null
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
    Timber.v(
      "Discovery parsed -> $discoveryMessage (from %s:%d)",
      packet.address?.hostAddress,
      packet.port
    )
    if (discoveryMessage != null && discoveryMessage.address.isEmpty()) {
      Timber.w("Received discovery message with empty address, using sender address")
      return discoveryMessage.copy(address = packet.address?.hostAddress.orEmpty())
    }
    return discoveryMessage
  }

  private class SocketCreationFailedException(cause: Throwable) : IOException(cause)

  companion object {
    private const val BUFFER_SIZE = 1024
    private const val NOTIFY = "notify"
    private const val MULTICAST_PORT = 45345
    private const val DISCOVERY_ADDRESS = "239.1.5.10"
  }
}
