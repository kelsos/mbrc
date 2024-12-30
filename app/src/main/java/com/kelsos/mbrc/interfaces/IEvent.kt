package com.kelsos.mbrc.interfaces

interface IEvent {
  val type: String

  val data: Any

  val dataString: String
}
