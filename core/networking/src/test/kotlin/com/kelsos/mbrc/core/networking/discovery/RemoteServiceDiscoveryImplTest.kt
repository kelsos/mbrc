package com.kelsos.mbrc.core.networking.discovery

import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.wifi.WifiManager
import com.google.common.truth.Truth.assertThat
import com.kelsos.mbrc.core.common.test.testDispatcher
import com.kelsos.mbrc.core.networking.LocalNetworkAccess
import com.squareup.moshi.Moshi
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.test.runTest
import org.junit.Test

/** Covers the two guards [RemoteServiceDiscoveryImpl.discover] applies before it opens a socket. */
class RemoteServiceDiscoveryImplTest {
  private val wifiManager: WifiManager = mockk(relaxed = true)
  private val connectivityManager: ConnectivityManager = mockk()
  private val moshi: Moshi = Moshi.Builder().build()

  private fun discovery(permitted: Boolean) = RemoteServiceDiscoveryImpl(
    wifiManager,
    connectivityManager,
    LocalNetworkAccess { permitted },
    moshi,
    mockk(relaxed = true),
    DiscoveryTiming.SHIPPED
  )

  private fun onWifi(connected: Boolean) {
    val network: Network = mockk()
    val capabilities: NetworkCapabilities = mockk {
      every { hasTransport(NetworkCapabilities.TRANSPORT_WIFI) } returns connected
    }
    every { connectivityManager.activeNetwork } returns network
    every { connectivityManager.getNetworkCapabilities(network) } returns capabilities
  }

  /**
   * Distinct from [DiscoveryStop.NotFound] on purpose: nothing was searched, and reporting "no
   * hosts found" would blame the network for a permission the user can grant.
   */
  @Test
  fun `without local network access the scan reports the permission, not an empty result`() =
    runTest(testDispatcher) {
      val result = discovery(permitted = false).discover()

      assertThat(result).isEqualTo(DiscoveryStop.LocalNetworkDenied)
    }

  @Test
  fun `a denied permission stops before anything touches the network`() = runTest(testDispatcher) {
    discovery(permitted = false).discover()

    verify(exactly = 0) { connectivityManager.activeNetwork }
    verify(exactly = 0) { wifiManager.createMulticastLock(any()) }
  }

  @Test
  fun `without wifi the scan reports no wifi`() = runTest(testDispatcher) {
    onWifi(connected = false)

    val result = discovery(permitted = true).discover()

    assertThat(result).isEqualTo(DiscoveryStop.NoWifi)
  }

  @Test
  fun `a connection without an active network counts as no wifi`() = runTest(testDispatcher) {
    every { connectivityManager.activeNetwork } returns null

    val result = discovery(permitted = true).discover()

    assertThat(result).isEqualTo(DiscoveryStop.NoWifi)
  }

  @Test
  fun `a network with no reported capabilities counts as no wifi`() = runTest(testDispatcher) {
    val network: Network = mockk()
    every { connectivityManager.activeNetwork } returns network
    every { connectivityManager.getNetworkCapabilities(network) } returns null

    val result = discovery(permitted = true).discover()

    assertThat(result).isEqualTo(DiscoveryStop.NoWifi)
  }

  @Test
  fun `no multicast lock is taken when there is no wifi`() = runTest(testDispatcher) {
    onWifi(connected = false)

    discovery(permitted = true).discover()

    verify(exactly = 0) { wifiManager.createMulticastLock(any()) }
  }
}
