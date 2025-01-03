package com.kelsos.mbrc.networking.client

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
class GenericSocketMessage<T>(
  @Json(name = "context")
  val context: String,
  @Json(name = "data")
  val data: T,
) where T : Any
