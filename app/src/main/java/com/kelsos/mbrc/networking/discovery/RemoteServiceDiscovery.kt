package com.kelsos.mbrc.networking.discovery

import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.wifi.WifiManager
import com.fasterxml.jackson.databind.ObjectMapper
import com.kelsos.mbrc.events.bus.RxBus
import com.kelsos.mbrc.events.ui.DiscoveryStopped
import com.kelsos.mbrc.features.settings.ConnectionRepository
import com.kelsos.mbrc.networking.connections.ConnectionMapper
import com.kelsos.mbrc.networking.protocol.Protocol
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import timber.log.Timber
import java.io.IOException
import java.net.DatagramPacket
import java.net.InetAddress
import java.net.MulticastSocket
import java.util.Locale
import javax.inject.Inject

class RemoteServiceDiscovery
  @Inject
  internal constructor(
    private val manager: WifiManager,
    private val connectivityManager: ConnectivityManager,
    private val mapper: ObjectMapper,
    private val bus: RxBus,
    private val connectionRepository: ConnectionRepository,
  ) {
    private var job = SupervisorJob()
    private var scope = CoroutineScope(job + Dispatchers.IO)
    private val connectionMapper = ConnectionMapper()
    private var multiCastLock: WifiManager.MulticastLock? = null
    private var group: InetAddress? = null
    private var isRunning: Boolean = false

    fun startDiscovery() {
      if (isRunning) {
        return
      }

      if (!isWifiConnected()) {
        bus.post(DiscoveryStopped(DiscoveryStop.NO_WIFI))
        return
      }
      isRunning = true
      multiCastLock =
        manager.createMulticastLock("locked").apply {
          setReferenceCounted(true)
          acquire()
        }

      Timber.Forest.v("Starting remote service discovery")
      scope.launch {
        try {
          discover()
        } catch (e: Exception) {
          Timber.Forest.v(e, "Discovery failed")
          bus.post(DiscoveryStopped(DiscoveryStop.NOT_FOUND))
        } finally {
          stopDiscovery()
          isRunning = false
        }
      }
    }

    private suspend fun discover() {
      var tries = 1
      val socket = createSocket()

      var discoveryMessage = getDiscoveryMessage(socket)
      while (discoveryMessage?.context != NOTIFY && tries < 3) {
        delay(3000)
        tries++
        Timber.Forest.v("Discovery try: %s", tries)

        discoveryMessage = getDiscoveryMessage(socket)
      }

      if (discoveryMessage?.context == NOTIFY) {
        connectionRepository.save(connectionMapper.map(discoveryMessage))
        bus.post(DiscoveryStopped(DiscoveryStop.COMPLETE))
      } else {
        bus.post(DiscoveryStopped(DiscoveryStop.NOT_FOUND))
      }
      cleanup(socket)
    }

    private fun getDiscoveryMessage(socket: MulticastSocket): DiscoveryMessage? =
      try {
        socket.discoveryMessage()
      } catch (e: Exception) {
        null
      }

    private fun stopDiscovery() {
      multiCastLock?.release()
      multiCastLock = null
    }

    private fun getWifiAddress(): String {
      val wifiInfo = manager.connectionInfo
      val address = wifiInfo.ipAddress
      return String.Companion.format(
        Locale.getDefault(),
        "%d.%d.%d.%d",
        address and 0xff,
        address shr 8 and 0xff,
        address shr 16 and 0xff,
        address shr 24 and 0xff,
      )
    }

    private fun isWifiConnected(): Boolean {
      val network = connectivityManager.activeNetwork ?: return false
      val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false
      return capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)
    }

    private fun cleanup(socket: MulticastSocket) {
      try {
        socket.leaveGroup(group)
        socket.close()
      } catch (e: IOException) {
        Timber.Forest.v("While cleaning up the discovery %s", e.message)
      }
    }

    private fun MulticastSocket.discoveryMessage(): DiscoveryMessage? {
      val buffer = ByteArray(512)
      val packet = DatagramPacket(buffer, buffer.size)
      receive(packet)
      val incomingMessage = String(packet.data, Charsets.UTF_8)
      val discoveryMessage = mapper.readValue(incomingMessage, DiscoveryMessage::class.java)
      Timber.Forest.v("Discovery: Received -> $discoveryMessage")
      return discoveryMessage
    }

    private fun createSocket(): MulticastSocket {
      try {
        group = InetAddress.getByName(DISCOVERY_ADDRESS)
        return MulticastSocket(MULTICASTPORT).apply {
          soTimeout = SO_TIMEOUT
          joinGroup(group)
          val data =
            mapper.writeValueAsBytes(
              DiscoveryMessage().apply {
                context = Protocol.DISCOVERY
                address = getWifiAddress()
              },
            )
          send(DatagramPacket(data, data.size, group, MULTICASTPORT))
        }
      } catch (e: IOException) {
        Timber.Forest.v(e, "Failed to open multi cast socket")
        throw RuntimeException(e)
      }
    }

    companion object {
      private const val NOTIFY = "notify"
      private const val SO_TIMEOUT = 15 * 1000
      private const val MULTICASTPORT = 45345
      private const val DISCOVERY_ADDRESS = "239.1.5.10" // NOPMD
    }
  }
