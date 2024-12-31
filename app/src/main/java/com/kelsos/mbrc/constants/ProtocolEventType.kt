package com.kelsos.mbrc.constants

object ProtocolEventType {
  const val INITIATE_PROTOCOL_REQUEST = "InitiateProtocolRequest"
  const val REDUCE_VOLUME = "ReduceVolume"
  const val HANDSHAKE_COMPLETE = "HandshakeComplete"
  const val INFORM_CLIENT_NOT_ALLOWED = "InformClientNotAllowed"
  const val PLUGIN_UPDATE_AVAILABLE = "PluginUpdateAvailable"
  const val PLUGIN_UPDATE_REQUIRED = "PluginUpdateRequired"
  const val USER_ACTION = "UserAction"
  const val PLUGIN_VERSION_CHECK = "PluginVersionCheck"
}
