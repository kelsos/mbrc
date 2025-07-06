package com.kelsos.mbrc.networking.protocol

data class UserAction(val protocol: Protocol, val data: Any) {
  companion object {
    fun create(protocol: Protocol): UserAction = UserAction(protocol, true)

    fun create(protocol: Protocol, data: Any): UserAction = UserAction(protocol, data)

    fun toggle(protocol: Protocol): UserAction = UserAction(protocol, Protocol.TOGGLE)
  }
}
