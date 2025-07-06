package com.kelsos.mbrc.networking.connections

import com.kelsos.mbrc.common.data.Mapper
import com.kelsos.mbrc.features.settings.ConnectionSettings
import java.net.InetSocketAddress
import java.net.SocketAddress

object InetAddressMapper : Mapper<ConnectionSettings, SocketAddress> {
  override fun map(from: ConnectionSettings): SocketAddress =
    InetSocketAddress(from.address, from.port)
}

fun ConnectionSettings.toSocketAddress(): SocketAddress = InetAddressMapper.map(this)
