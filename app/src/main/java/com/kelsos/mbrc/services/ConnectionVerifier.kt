package com.kelsos.mbrc.services

interface ConnectionVerifier {
  suspend fun verify(): Boolean
}
