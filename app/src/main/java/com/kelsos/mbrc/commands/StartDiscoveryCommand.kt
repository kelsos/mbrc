package com.kelsos.mbrc.commands

import com.kelsos.mbrc.networking.discovery.RemoteServiceDiscovery
import com.kelsos.mbrc.networking.protocol.ProtocolAction
import com.kelsos.mbrc.networking.protocol.ProtocolMessage
import javax.inject.Inject

class StartDiscoveryCommand
  @Inject
  constructor(
    private val discovery: RemoteServiceDiscovery,
  ) : ProtocolAction {
    override fun execute(message: ProtocolMessage) {
      discovery.startDiscovery()
    }
  }
