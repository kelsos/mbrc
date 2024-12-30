package com.kelsos.mbrc.data

class UserAction(val context: String, val data: Any) {
  companion object {

    fun create(context: String): UserAction {
      return UserAction(context, true)
    }

    fun create(context: String, data: Any): UserAction {
      return UserAction(context, data)
    }
  }
}
