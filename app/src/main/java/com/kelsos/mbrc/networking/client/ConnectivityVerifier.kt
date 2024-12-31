package com.kelsos.mbrc.networking.client

interface ConnectivityVerifier {
  suspend fun verify(): Boolean
}
