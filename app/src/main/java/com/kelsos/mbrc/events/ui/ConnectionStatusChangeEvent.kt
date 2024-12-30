package com.kelsos.mbrc.events.ui

import com.kelsos.mbrc.annotations.Connection.Status

class ConnectionStatusChangeEvent private constructor(
  @Status val status: Int,
) {
  companion object {
    fun create(
      @Status status: Int,
    ): ConnectionStatusChangeEvent = ConnectionStatusChangeEvent(status)
  }
}
