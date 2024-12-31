package com.kelsos.mbrc.events.ui

import com.fasterxml.jackson.databind.node.ObjectNode

class TrackRemoval(
  node: ObjectNode,
) {
  val index: Int = node.path("index").asInt()
  val isSuccess: Boolean = node.path("success").asBoolean()
}
