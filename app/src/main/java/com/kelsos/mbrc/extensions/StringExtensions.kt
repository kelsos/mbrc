package com.kelsos.mbrc.extensions

fun String.escapeLike(): String = this.replace("%", "_")
