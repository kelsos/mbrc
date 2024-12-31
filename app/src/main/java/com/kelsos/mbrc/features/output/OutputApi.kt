package com.kelsos.mbrc.features.output

interface OutputApi {
  suspend fun getOutputs(): OutputResponse

  suspend fun setOutput(active: String): OutputResponse
}
