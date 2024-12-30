package com.kelsos.mbrc.events.ui

import com.fasterxml.jackson.databind.node.ObjectNode

class TrackRemoval(
  node: ObjectNode,
) {
  val index: Int
  val isSuccess: Boolean

  init {
    index = node.path("index").asInt()
    isSuccess = node.path("success").asBoolean()
  }
}
