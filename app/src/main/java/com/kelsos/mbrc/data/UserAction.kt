package com.kelsos.mbrc.data

class UserAction(
  val context: String,
  val data: Any,
) {
  companion object {
    fun create(context: String): UserAction = UserAction(context, true)

    fun create(
      context: String,
      data: Any,
    ): UserAction = UserAction(context, data)
  }
}
