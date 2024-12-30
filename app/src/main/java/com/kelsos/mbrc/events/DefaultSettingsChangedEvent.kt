package com.kelsos.mbrc.events

class DefaultSettingsChangedEvent {

  companion object {
    fun create(): DefaultSettingsChangedEvent {
      return DefaultSettingsChangedEvent()
    }
  }
}
