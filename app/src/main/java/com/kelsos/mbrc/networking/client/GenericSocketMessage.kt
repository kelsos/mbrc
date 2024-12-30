package com.kelsos.mbrc.networking.client

import com.fasterxml.jackson.annotation.JsonProperty

class GenericSocketMessage<T>(
  @param:JsonProperty
  var context: String,
  @param:JsonProperty
  var data: T,
) where T : Any
