package com.kelsos.mbrc.common.data

internal interface Mapper<From, To> {
  fun map(from: From): To
}
