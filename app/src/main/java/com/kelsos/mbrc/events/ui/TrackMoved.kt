package com.kelsos.mbrc.events.ui

import com.fasterxml.jackson.databind.node.ObjectNode

class TrackMoved(node: ObjectNode) {
  val isSuccess: Boolean
  val from: Int
  val to: Int

  init {
    isSuccess = node.path("success").asBoolean()
    from = node.path("from").asInt()
    to = node.path("to").asInt()
  }
}
