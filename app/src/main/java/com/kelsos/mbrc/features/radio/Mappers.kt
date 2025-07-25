package com.kelsos.mbrc.features.radio

import com.kelsos.mbrc.common.data.Mapper

object RadioDtoMapper : Mapper<RadioStationDto, RadioStationEntity> {
  override fun map(from: RadioStationDto): RadioStationEntity =
    RadioStationEntity(from.name, from.url)
}

object RadioDaoMapper : Mapper<RadioStationEntity, RadioStation> {
  override fun map(from: RadioStationEntity): RadioStation = RadioStation(
    name = from.name,
    url = from.url,
    id = from.id
  )
}

fun RadioStationDto.toEntity(): RadioStationEntity = RadioDtoMapper.map(this)

fun RadioStationEntity.toRadioStation(): RadioStation = RadioDaoMapper.map(this)
