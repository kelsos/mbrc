package com.kelsos.mbrc.mappers

internal interface Mapper<From, To> {
  fun map(from: From): To
}
