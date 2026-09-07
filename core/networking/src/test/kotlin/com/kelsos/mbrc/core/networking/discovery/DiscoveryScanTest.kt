package com.kelsos.mbrc.core.networking.discovery

import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.wifi.WifiManager
import com.google.common.truth.Truth.assertThat
import com.google.common.truth.Truth.assertWithMessage
import com.kelsos.mbrc.core.common.test.coroutineTestTimeout
import com.kelsos.mbrc.core.networking.LocalNetworkAccess
import com.kelsos.mbrc.core.networking.protocol.Clock
import com.squareup.moshi.Moshi
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import java.io.IOException
import java.net.DatagramPacket
import java.net.InetAddress
import java.net.MulticastSocket
import java.net.SocketException
import java.net.SocketTimeoutException
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicLong
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.junit.Rule
import org.junit.Test
import org.junit.rules.Timeout

/**
 * Drives a full scan over a fake LAN: the scan binds a [FakeMulticastSocket] whose reads come from
 * a queue the test fills, so nothing leaves the machine. Only the two JDK calls that cannot be
 * substituted are stubbed — binding a socket and listing the host's interfaces. Everything the scan
 * decides (which port, which timeout, which group, which address to announce) is the real code, and
 * the socket records it.
 *
 * No test waits on real time. The scan reads the clock it is given, and [FakeMulticastSocket]
 * advances that clock by the socket timeout on every read, which is what a real socket costs when
 * nobody answers. The window therefore closes after a fixed number of reads, and the timings in
 * [TEST_TIMING] are chosen to make those counts easy to state.
 */
class DiscoveryScanTest {

  @get:Rule
  val timeout: Timeout = coroutineTestTimeout()

  private val wifiManager: WifiManager = mockk(relaxed = true)
  private val connectivityManager: ConnectivityManager = mockk()
  private val clock = FakeClock()
  private val socket = FakeMulticastSocket(clock, TEST_TIMING.responseTimeoutMs.toLong())

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

    assertWithMessage("a 100ms window re-broadcasting every 40ms announces at 0, 40 and 80")
      .that(socket.sent)
      .hasSize(3)
  }

  @Test
  fun `the scan stops announcing once a host has answered`() {
    socket.answer(notify(address = "192.168.1.55", port = 3000, name = "Desktop"))

    scan()

    assertWithMessage("the window closes 30ms after the answer, before the first re-broadcast")
      .that(socket.sent)
      .hasSize(1)
  }

  @Test
  fun `the scan closes its window instead of listening forever`() {
    scan()

    assertThat(clock.now()).isEqualTo(TEST_TIMING.collectionWindowMs)
  }

  @Test
  fun `a socket that cannot be bound reports nothing found`() {
    val result = scan(sockets = { throw IOException("no route") })

    assertThat(result).isEqualTo(DiscoveryStop.NotFound)
  }

  @Test
  fun `the socket is bound to the discovery port and joined to the discovery group`() {
    scan()

    assertThat(socket.boundPort).isEqualTo(MULTICAST_PORT)
    assertThat(socket.joined?.hostAddress).isEqualTo("239.1.5.10")
  }

  @Test
  fun `the read timeout comes from the configured timing`() {
    scan()

    assertThat(socket.readTimeout).isEqualTo(TEST_TIMING.responseTimeoutMs)
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
   * not pin the network thread for the rest of the window. The socket here costs no time, so the
   * window never closes on its own: only the cancellation can end this scan.
   */
  @Test
  fun `a cancelled scan releases the socket instead of running the window out`() = runBlocking {
    val endless = FakeMulticastSocket(clock, readCostMs = 0)
    onWifi(connected = true)
    val job = CoroutineScope(Dispatchers.Default).launch {
      discovery(sockets = { endless }).discover()
    }

    while (endless.reads.get() == 0) {
      Thread.yield()
    }
    job.cancelAndJoin()

    assertThat(endless.closed).isTrue()
  }

  @Test
  fun `a wifi connection with no ipv4 address reports nothing found`() {
    val result = scan(interfaces = { listOf(wired(addresses = listOf(address("::1")))) })

    assertThat(result).isEqualTo(DiscoveryStop.NotFound)
  }

  @Test
  fun `no socket is bound when there is no address to announce`() {
    scan(interfaces = { emptyList() })

    assertThat(socket.boundPort).isNull()
    assertThat(socket.sent).isEmpty()
  }

  @Test
  fun `an interface that is down cannot carry the announcement`() {
    val result = scan(
      interfaces = {
        listOf(
          wired(isUp = false, addresses = listOf(address("192.168.1.10"))),
          wired(addresses = listOf(address("192.168.1.20")))
        )
      }
    )

    assertThat(result).isEqualTo(DiscoveryStop.NotFound)
    assertThat(socket.sent.first()).contains("\"address\":\"192.168.1.20\"")
  }

  @Test
  fun `the loopback interface cannot carry the announcement`() {
    scan(
      interfaces = {
        listOf(
          wired(isLoopback = true, addresses = listOf(address("127.0.0.1"))),
          wired(addresses = listOf(address("192.168.1.20")))
        )
      }
    )

    assertThat(socket.sent.first()).contains("\"address\":\"192.168.1.20\"")
  }

  @Test
  fun `an ipv6 address is skipped in favour of the ipv4 one behind it`() {
    scan(
      interfaces = {
        listOf(wired(addresses = listOf(address("fe80::1"), address("192.168.1.30"))))
      }
    )

    assertThat(socket.sent.first()).contains("\"address\":\"192.168.1.30\"")
  }

  @Test
  fun `an unreadable interface list reports nothing found`() {
    val result = scan(interfaces = { throw SocketException("interface list unavailable") })

    assertThat(result).isEqualTo(DiscoveryStop.NotFound)
  }

  private fun scan(
    sockets: MulticastSockets = MulticastSockets { port -> socket.also { it.boundPort = port } },
    interfaces: NetworkInterfaces = NetworkInterfaces { listOf(wired()) }
  ): DiscoveryStop {
    onWifi(connected = true)
    return runBlocking { discovery(sockets, interfaces).discover() }
  }

  private fun discovery(
    sockets: MulticastSockets,
    interfaces: NetworkInterfaces = NetworkInterfaces { listOf(wired()) }
  ) = RemoteServiceDiscoveryImpl(
    wifiManager,
    connectivityManager,
    LocalNetworkAccess { true },
    MOSHI,
    sockets,
    interfaces,
    TEST_TIMING,
    clock
  )

  private fun wired(
    isUp: Boolean = true,
    isLoopback: Boolean = false,
    addresses: List<InetAddress> = listOf(address(LOCAL_ADDRESS))
  ) = LocalInterface(isUp = isUp, isLoopback = isLoopback, addresses = addresses)

  private fun address(literal: String): InetAddress = InetAddress.getByName(literal)

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

  private class FakeClock : Clock {
    private val millis = AtomicLong()

    override fun now(): Long = millis.get()

    fun advance(by: Long) {
      millis.addAndGet(by)
    }
  }

  /**
   * Binds an ephemeral local port so the JDK is happy, then answers every read from [inbound]
   * without touching the network. An empty queue reads as a socket timeout, which is what the real
   * socket does between responses, and costs the same [DiscoveryTiming.responseTimeoutMs] on
   * [clock] that a real read would cost on the wall clock.
   */
  private class FakeMulticastSocket(private val clock: FakeClock, private val readCostMs: Long) :
    MulticastSocket(0) {
    private val inbound = ConcurrentLinkedQueue<Response>()

    val sent = CopyOnWriteArrayList<String>()
    val reads = AtomicInteger()

    @Volatile
    var closed = false

    @Volatile
    var leftGroup = false

    @Volatile
    var boundPort: Int? = null

    @Volatile
    var joined: InetAddress? = null

    @Volatile
    var readTimeout: Int? = null

    override fun setSoTimeout(timeout: Int) {
      readTimeout = timeout
    }

    override fun joinGroup(group: InetAddress) {
      joined = group
    }

    fun answer(json: String, from: String = "192.168.1.55") {
      inbound += Response(json.toByteArray(), InetAddress.getByName(from))
    }

    override fun send(p: DatagramPacket) {
      sent += String(p.data, p.offset, p.length)
    }

    override fun receive(p: DatagramPacket) {
      reads.incrementAndGet()
      val response = inbound.poll() ?: run {
        clock.advance(readCostMs)
        throw SocketTimeoutException("nothing queued")
      }
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
      responseTimeoutMs = 10,
      collectionWindowMs = 100,
      postFirstGatherMs = 30,
      rebroadcastIntervalMs = 40
    )
  }
}
