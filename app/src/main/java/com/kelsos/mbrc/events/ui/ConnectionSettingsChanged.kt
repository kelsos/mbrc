package com.kelsos.mbrc.events.ui

class ConnectionSettingsChanged private constructor(
  val defaultId: Long,
) {
  companion object {
    fun newInstance(defaultId: Long): ConnectionSettingsChanged = ConnectionSettingsChanged(defaultId)
  }
}
