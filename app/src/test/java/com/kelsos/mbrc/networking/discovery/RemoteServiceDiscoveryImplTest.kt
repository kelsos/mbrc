package com.kelsos.mbrc.networking.discovery

import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.wifi.WifiManager
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import com.kelsos.mbrc.networking.connections.toConnection
import com.squareup.moshi.Moshi
import io.mockk.Runs
import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.verify
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.core.module.dsl.bind
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.module
import org.koin.test.KoinTest
import org.koin.test.inject
import java.io.IOException

@RunWith(AndroidJUnit4::class)
class RemoteServiceDiscoveryImplTest : KoinTest {
  private val testDispatcher = StandardTestDispatcher()
  private val mockWifiManager: WifiManager = mockk(relaxed = true)
  private val mockConnectivityManager: ConnectivityManager = mockk(relaxed = true)
  private val mockMulticastLock: WifiManager.MulticastLock = mockk(relaxed = true)
  private val mockNetwork: Network = mockk()
  private val mockNetworkCapabilities: NetworkCapabilities = mockk()

  private val testModule =
    module {
      single { mockWifiManager }
      single { mockConnectivityManager }
      single { Moshi.Builder().build() }
      singleOf(::RemoteServiceDiscoveryImpl) {
        bind<RemoteServiceDiscovery>()
      }
    }

  private val discovery: RemoteServiceDiscovery by inject()

  @Before
  fun setUp() {
    startKoin { modules(listOf(testModule)) }

    // Setup default WiFi manager mocks
    every { mockWifiManager.createMulticastLock(any()) } returns mockMulticastLock
    every { mockMulticastLock.setReferenceCounted(any()) } just Runs
    every { mockMulticastLock.acquire() } just Runs
    every { mockMulticastLock.release() } just Runs
    every { mockMulticastLock.isHeld } returns true
  }

  @After
  fun tearDown() {
    stopKoin()
    clearAllMocks()
  }

  @Test
  fun testNoWifiConnectionReturnNoWifi() {
    runTest(testDispatcher) {
      // Given: No WiFi connection
      setupNoWiFi()

      // When: discovery is performed
      val result = discovery.discover()

      // Then: should return NoWifi
      assertThat(result).isEqualTo(DiscoveryStop.NoWifi)
    }
  }

  @Test
  fun testNoActiveNetworkReturnNoWifi() {
    runTest(testDispatcher) {
      // Given: No active network
      every { mockConnectivityManager.activeNetwork } returns null

      // When: discovery is performed
      val result = discovery.discover()

      // Then: should return NoWifi
      assertThat(result).isEqualTo(DiscoveryStop.NoWifi)
    }
  }

  @Test
  fun testNoNetworkCapabilitiesReturnNoWifi() {
    runTest(testDispatcher) {
      // Given: Active network but no capabilities
      every { mockConnectivityManager.activeNetwork } returns mockNetwork
      every { mockConnectivityManager.getNetworkCapabilities(mockNetwork) } returns null

      // When: discovery is performed
      val result = discovery.discover()

      // Then: should return NoWifi
      assertThat(result).isEqualTo(DiscoveryStop.NoWifi)
    }
  }

  @Test
  fun testNonWifiTransportReturnNoWifi() {
    runTest(testDispatcher) {
      // Given: Active network with non-WiFi transport
      every { mockConnectivityManager.activeNetwork } returns mockNetwork
      every { mockConnectivityManager.getNetworkCapabilities(mockNetwork) } returns mockNetworkCapabilities
      every { mockNetworkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) } returns false

      // When: discovery is performed
      val result = discovery.discover()

      // Then: should return NoWifi
      assertThat(result).isEqualTo(DiscoveryStop.NoWifi)
    }
  }

  @Test
  fun testMulticastLockManagement() {
    runTest(testDispatcher) {
      // Given: WiFi connected but socket creation will fail
      setupWifiConnected()

      // Mock static methods to cause IOException during socket operations
      mockkStatic("java.net.InetAddress")
      every { java.net.InetAddress.getByName(any()) } throws IOException("Mock socket error")

      try {
        // When: discovery is attempted
        discovery.discover()
      } catch (_: Exception) {
        // Expected to fail, but that's ok for this test
      }

      // Then: multicast lock should be properly managed
      verify { mockWifiManager.createMulticastLock("locked") }
      verify { mockMulticastLock.acquire() }
      verify { mockMulticastLock.release() }
    }
  }

  @Test
  fun testDiscoveryMessageJsonParsing() {
    // Given: A Moshi adapter
    val moshi = Moshi.Builder().build()
    val adapter = moshi.adapter(DiscoveryMessage::class.java)

    // When: parsing a valid discovery message
    val validJson = """{"name":"MusicBee","address":"192.168.1.100","port":3000,"context":"notify"}"""
    val message = adapter.fromJson(validJson)

    // Then: should parse correctly
    assertThat(message).isNotNull()
    assertThat(message!!.name).isEqualTo("MusicBee")
    assertThat(message.address).isEqualTo("192.168.1.100")
    assertThat(message.port).isEqualTo(3000)
    assertThat(message.context).isEqualTo("notify")
  }

  @Test
  fun testDiscoveryMessageJsonParsingInvalid() {
    // Given: A Moshi adapter
    val moshi = Moshi.Builder().build()
    val adapter = moshi.adapter(DiscoveryMessage::class.java)

    // When: parsing invalid JSON
    val invalidJson = "invalid json"

    // Then: should throw exception or return null
    try {
      val message = adapter.fromJson(invalidJson)
      assertThat(message).isNull()
    } catch (e: Exception) {
      // Expected behavior for invalid JSON
      assertThat(e).isNotNull()
    }
  }

  @Test
  fun testDiscoveryMessageToConnectionConversion() {
    // Given: A valid discovery message
    val message =
      DiscoveryMessage(
        name = "TestMusicBee",
        address = "192.168.1.200",
        port = 3001,
        context = "notify",
      )

    // When: converting to connection settings
    val connection = message.toConnection()

    // Then: should create correct connection settings
    assertThat(connection.name).isEqualTo("TestMusicBee")
    assertThat(connection.address).isEqualTo("192.168.1.200")
    assertThat(connection.port).isEqualTo(3001)
  }

  private fun setupNoWiFi() {
    every { mockConnectivityManager.activeNetwork } returns mockNetwork
    every { mockConnectivityManager.getNetworkCapabilities(mockNetwork) } returns mockNetworkCapabilities
    every { mockNetworkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) } returns false
  }

  private fun setupWifiConnected() {
    every { mockConnectivityManager.activeNetwork } returns mockNetwork
    every { mockConnectivityManager.getNetworkCapabilities(mockNetwork) } returns mockNetworkCapabilities
    every { mockNetworkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) } returns true
  }
}
