package com.kelsos.mbrc.mappers

import com.kelsos.mbrc.data.ConnectionSettings
import java.net.InetSocketAddress
import java.net.SocketAddress

class InetAddressMapper : Mapper<ConnectionSettings, SocketAddress> {
  override fun map(from: ConnectionSettings): SocketAddress {
    return InetSocketAddress(from.address, from.port)
  }
}
