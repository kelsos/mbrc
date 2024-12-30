package com.kelsos.mbrc.events.ui

class NotifyUser {
  val message: String
  val resId: Int
  var isFromResource: Boolean = false
    private set

  constructor(message: String) {
    this.message = message
    this.isFromResource = false
    this.resId = -1
  }

  constructor(resId: Int) {
    this.resId = resId
    this.isFromResource = true
    this.message = ""
  }
}
