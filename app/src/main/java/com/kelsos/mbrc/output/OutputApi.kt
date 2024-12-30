package com.kelsos.mbrc.output

interface OutputApi {
  suspend fun getOutputs(): OutputResponse

  suspend fun setOutput(active: String): OutputResponse
}
