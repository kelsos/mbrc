package com.kelsos.mbrc.networking.client

data class ResponseWithPayload<P, T>(
  val payload: P,
  val response: T
)
