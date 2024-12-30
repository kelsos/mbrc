package com.kelsos.mbrc.events.ui

class VolumeChange {
  var volume: Int = 0
    private set
  var isMute: Boolean = false
    private set

  constructor(vol: Int) {
    this.volume = vol
    this.isMute = false
  }

  constructor() {
    this.volume = 0
    this.isMute = true
  }
}
