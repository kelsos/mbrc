package com.kelsos.mbrc.networking.connections

import com.kelsos.mbrc.interfaces.data.Mapper
import java.net.InetSocketAddress
import java.net.SocketAddress

class InetAddressMapper : Mapper<ConnectionSettings, SocketAddress> {
  override fun map(from: ConnectionSettings): SocketAddress {
    return InetSocketAddress(from.address, from.port)
  }
}
