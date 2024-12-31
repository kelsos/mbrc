package com.kelsos.mbrc.networking.connections

import com.kelsos.mbrc.common.data.Mapper
import com.kelsos.mbrc.features.settings.ConnectionSettings
import com.kelsos.mbrc.networking.discovery.DiscoveryMessage

class ConnectionMapper : Mapper<DiscoveryMessage, ConnectionSettings> {
  override fun map(from: DiscoveryMessage): ConnectionSettings {
    val settings = ConnectionSettings()
    settings.address = from.address
    settings.port = from.port
    settings.name = from.name
    return settings
  }
}
