package com.kelsos.mbrc.core.networking.discovery

import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.wifi.WifiManager
import com.google.common.truth.Truth.assertThat
import com.google.common.truth.Truth.assertWithMessage
import com.kelsos.mbrc.core.common.test.coroutineTestTimeout
import com.kelsos.mbrc.core.networking.LocalNetworkAccess
import com.squareup.moshi.Moshi
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import java.io.IOException
import java.net.DatagramPacket
import java.net.InetAddress
import java.net.MulticastSocket
import java.net.SocketTimeoutException
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.atomic.AtomicInteger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.junit.Rule
import org.junit.Test
import org.junit.rules.Timeout

/**
 * Drives a full scan over a fake LAN: [FakeDiscoveryNetwork] hands the scan a socket whose reads
 * come from a queue the test fills, so nothing leaves the machine.
 *
 * The scan measures wall-clock time, so these run on real time with the window shortened to
 * [TEST_TIMING]. `runTest` is deliberately not used: its virtual clock would not move
 * `System.currentTimeMillis`, and the collection loop would never close.
 */
class DiscoveryScanTest {

  @get:Rule
  val timeout: Timeout = coroutineTestTimeout()

  private val wifiManager: WifiManager = mockk(relaxed = true)
  private val connectivityManager: ConnectivityManager = mockk()
  private val socket = FakeMulticastSocket()

  @Test
  fun `a notify response is reported as a discovered host`() {
    socket.answer(notify(address = "192.168.1.55", port = 3000, name = "Desktop"))

    val result = scan()

    val settings = (result as DiscoveryStop.Complete).settings
    assertThat(settings).hasSize(1)
    assertThat(settings.first().address).isEqualTo("192.168.1.55")
    assertThat(settings.first().port).isEqualTo(3000)
    assertThat(settings.first().name).isEqualTo("Desktop")
  }

  @Test
  fun `two hosts answering the same broadcast are both reported`() {
    socket.answer(notify(address = "192.168.1.55", port = 3000, name = "Desktop"))
    socket.answer(notify(address = "192.168.1.56", port = 3000, name = "Laptop"))

    val settings = (scan() as DiscoveryStop.Complete).settings

    assertThat(settings.map { it.address })
      .containsExactly("192.168.1.55", "192.168.1.56")
      .inOrder()
  }

  @Test
  fun `a host answering twice is reported once`() {
    socket.answer(notify(address = "192.168.1.55", port = 3000, name = "Desktop"))
    socket.answer(notify(address = "192.168.1.55", port = 3000, name = "Desktop"))

    val settings = (scan() as DiscoveryStop.Complete).settings

    assertWithMessage("responses de-duplicate by address and port")
      .that(settings)
      .hasSize(1)
  }

  @Test
  fun `the same address on another port is a separate host`() {
    socket.answer(notify(address = "192.168.1.55", port = 3000, name = "Desktop"))
    socket.answer(notify(address = "192.168.1.55", port = 3001, name = "Desktop second instance"))

    val settings = (scan() as DiscoveryStop.Complete).settings

    assertThat(settings.map { it.port }).containsExactly(3000, 3001)
  }

  @Test
  fun `a response that is not a notify is ignored`() {
    socket.answer(message(context = "discovery", address = "192.168.1.55", port = 3000))

    assertThat(scan()).isEqualTo(DiscoveryStop.NotFound)
  }

  @Test
  fun `a host that reports no address is taken from the packet it sent`() {
    socket.answer(notify(address = "", port = 3000, name = "Desktop"), from = "192.168.1.77")

    val settings = (scan() as DiscoveryStop.Complete).settings

    assertThat(settings.first().address).isEqualTo("192.168.1.77")
  }

  @Test
  fun `a malformed response does not end the scan`() {
    socket.answer("not json at all")
    socket.answer(notify(address = "192.168.1.55", port = 3000, name = "Desktop"))

    val settings = (scan() as DiscoveryStop.Complete).settings

    assertThat(settings.map { it.address }).containsExactly("192.168.1.55")
  }

  @Test
  fun `an empty lan reports nothing found`() {
    assertThat(scan()).isEqualTo(DiscoveryStop.NotFound)
  }

  @Test
  fun `the scan announces itself before listening`() {
    scan()

    assertThat(socket.sent).isNotEmpty()
    assertThat(socket.sent.first()).contains("\"context\":\"discovery\"")
    assertThat(socket.sent.first()).contains("\"address\":\"$LOCAL_ADDRESS\"")
  }

  @Test
  fun `the announcement is repeated while nobody answers`() {
    scan()

    assertWithMessage("a single dropped packet should not hide a host for the whole window")
      .that(socket.sent.size)
      .isGreaterThan(1)
  }

  @Test
  fun `the scan stops announcing once a host has answered`() {
    socket.answer(notify(address = "192.168.1.55", port = 3000, name = "Desktop"))

    scan()

    assertWithMessage("the window closes shortly after the first answer")
      .that(socket.sent.size)
      .isLessThan((TEST_TIMING.collectionWindowMs / TEST_TIMING.rebroadcastIntervalMs).toInt())
  }

  @Test
  fun `a socket that cannot be opened reports nothing found`() {
    val result = scan(network = FakeDiscoveryNetwork(socket, failure = IOException("no route")))

    assertThat(result).isEqualTo(DiscoveryStop.NotFound)
  }

  @Test
  fun `the socket is released when the scan ends`() {
    scan()

    assertThat(socket.leftGroup).isTrue()
    assertThat(socket.closed).isTrue()
  }

  @Test
  fun `the multicast lock is taken for the scan and released after it`() {
    val lock: WifiManager.MulticastLock = mockk(relaxed = true) {
      every { isHeld } returns true
    }
    every { wifiManager.createMulticastLock(any()) } returns lock

    scan()

    verify { lock.acquire() }
    verify { lock.release() }
  }

  /**
   * The loop yields between reads so a caller that goes away (screen left, scope torn down) does
   * not pin the network thread for the rest of the window.
   */
  @Test
  fun `a cancelled scan releases the socket instead of running the window out`() = runBlocking {
    val scope = CoroutineScope(Dispatchers.Default)
    onWifi(connected = true)
    val job = scope.launch { discovery(FakeDiscoveryNetwork(socket)).discover() }

    while (socket.reads.get() == 0) {
      Thread.yield()
    }
    job.cancelAndJoin()

    assertThat(socket.closed).isTrue()
  }

  /**
   * Pins current behaviour rather than endorsing it: a wifi connection whose interfaces carry no
   * IPv4 address fails the scan outright instead of reporting [DiscoveryStop.NotFound].
   */
  @Test(expected = IllegalArgumentException::class)
  fun `a wifi connection with no ipv4 address fails the scan`() {
    scan(network = FakeDiscoveryNetwork(socket, address = null))
  }

  private fun scan(network: DiscoveryNetwork = FakeDiscoveryNetwork(socket)): DiscoveryStop {
    onWifi(connected = true)
    return runBlocking { discovery(network).discover() }
  }

  private fun discovery(network: DiscoveryNetwork) = RemoteServiceDiscoveryImpl(
    wifiManager,
    connectivityManager,
    LocalNetworkAccess { true },
    MOSHI,
    network,
    TEST_TIMING
  )

  private fun onWifi(connected: Boolean) {
    val network: Network = mockk()
    val capabilities: NetworkCapabilities = mockk {
      every { hasTransport(NetworkCapabilities.TRANSPORT_WIFI) } returns connected
    }
    every { connectivityManager.activeNetwork } returns network
    every { connectivityManager.getNetworkCapabilities(network) } returns capabilities
  }

  private fun notify(address: String, port: Int, name: String) =
    message(context = "notify", address = address, port = port, name = name)

  private fun message(context: String, address: String, port: Int, name: String = "") =
    """{"name":"$name","address":"$address","port":$port,"context":"$context"}"""

  private class FakeDiscoveryNetwork(
    private val socket: MulticastSocket,
    private val address: String? = LOCAL_ADDRESS,
    private val failure: IOException? = null
  ) : DiscoveryNetwork {
    override fun open(group: InetAddress, port: Int, timeoutMs: Int): MulticastSocket {
      failure?.let { throw it }
      return socket
    }

    override fun localAddress(): String? = address
  }

  /**
   * Binds an ephemeral local port so the JDK is happy, then answers every read from [inbound]
   * without touching the network. An empty queue reads as a socket timeout, which is what the real
   * socket does between responses.
   */
  private class FakeMulticastSocket : MulticastSocket(0) {
    private val inbound = ConcurrentLinkedQueue<Response>()

    val sent = CopyOnWriteArrayList<String>()
    val reads = AtomicInteger()

    @Volatile
    var closed = false

    @Volatile
    var leftGroup = false

    fun answer(json: String, from: String = "192.168.1.55") {
      inbound += Response(json.toByteArray(), InetAddress.getByName(from))
    }

    override fun send(p: DatagramPacket) {
      sent += String(p.data, p.offset, p.length)
    }

    override fun receive(p: DatagramPacket) {
      reads.incrementAndGet()
      val response = inbound.poll() ?: throw SocketTimeoutException("nothing queued")
      System.arraycopy(response.bytes, 0, p.data, p.offset, response.bytes.size)
      p.length = response.bytes.size
      p.address = response.from
      p.port = MULTICAST_PORT
    }

    override fun leaveGroup(group: InetAddress) {
      leftGroup = true
    }

    override fun close() {
      closed = true
      super.close()
    }

    private class Response(val bytes: ByteArray, val from: InetAddress)
  }

  private companion object {
    val MOSHI: Moshi = Moshi.Builder().build()
    const val LOCAL_ADDRESS = "192.168.1.10"
    const val MULTICAST_PORT = 45345

    val TEST_TIMING = DiscoveryTiming(
      responseTimeoutMs = 20,
      collectionWindowMs = 400,
      postFirstGatherMs = 40,
      rebroadcastIntervalMs = 60
    )
  }
}
