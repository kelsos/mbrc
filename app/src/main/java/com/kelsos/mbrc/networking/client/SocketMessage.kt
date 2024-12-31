package com.kelsos.mbrc.networking.client

import com.fasterxml.jackson.annotation.JsonProperty

class SocketMessage {
  @JsonProperty
  var context: String? = null

  @JsonProperty
  var data: Any? = null

  @SuppressWarnings("unused")
  constructor()

  private constructor(context: String, data: Any) {
    this.context = context
    this.data = data
  }

  companion object {
    fun create(
      context: String,
      data: Any,
    ): SocketMessage = SocketMessage(context, data)

    fun create(context: String): SocketMessage = SocketMessage(context, "")
  }
}
