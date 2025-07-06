package com.kelsos.mbrc.networking.discovery

import com.kelsos.mbrc.features.settings.ConnectionSettings

sealed class DiscoveryStop {
  object NoWifi : DiscoveryStop()

  object NotFound : DiscoveryStop()

  class Complete(val settings: ConnectionSettings) : DiscoveryStop()
}
