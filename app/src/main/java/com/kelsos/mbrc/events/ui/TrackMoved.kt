package com.kelsos.mbrc.events.ui

import com.fasterxml.jackson.databind.node.ObjectNode

class TrackMoved(
  node: ObjectNode,
) {
  val isSuccess: Boolean = node.path("success").asBoolean()
  val from: Int = node.path("from").asInt()
  val to: Int = node.path("to").asInt()
}
