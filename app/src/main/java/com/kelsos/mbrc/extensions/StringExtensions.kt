package com.kelsos.mbrc.extensions

fun String.escapeLike(): String {
  return this.replace("%", "_")
}
