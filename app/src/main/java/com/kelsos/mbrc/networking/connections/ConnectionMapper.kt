package com.kelsos.mbrc.networking.connections

import com.kelsos.mbrc.common.data.Mapper
import com.kelsos.mbrc.features.settings.ConnectionSettings
import com.kelsos.mbrc.networking.discovery.DiscoveryMessage

object ConnectionMapper : Mapper<DiscoveryMessage, ConnectionSettings> {
  override fun map(from: DiscoveryMessage): ConnectionSettings = ConnectionSettings(
    address = from.address,
    port = from.port,
    name = from.name,
    isDefault = false,
    0
  )
}

fun DiscoveryMessage.toConnection(): ConnectionSettings = ConnectionMapper.map(this)
