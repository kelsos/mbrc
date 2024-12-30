package com.kelsos.mbrc.repository.data

import com.kelsos.mbrc.constants.Protocol
import com.kelsos.mbrc.data.RadioStation
import com.kelsos.mbrc.networking.ApiBase
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class RemoteRadioDataSource
  @Inject
  constructor(
    private val service: ApiBase,
  ) : RemoteDataSource<RadioStation> {
    override suspend fun fetch(): Flow<List<RadioStation>> = service.getAllPages(Protocol.RadioStations, RadioStation::class)
  }
