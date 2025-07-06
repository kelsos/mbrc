package com.kelsos.mbrc.features.library.artists

import com.kelsos.mbrc.common.data.Mapper

object ArtistDtoMapper : Mapper<ArtistDto, ArtistEntity> {
  override fun map(from: ArtistDto): ArtistEntity = ArtistEntity(from.artist)
}

object ArtistEntityMapper : Mapper<ArtistEntity, Artist> {
  override fun map(from: ArtistEntity): Artist = Artist(
    id = from.id ?: 0,
    artist = from.artist.orEmpty()
  )
}

fun ArtistDto.toEntity(): ArtistEntity = ArtistDtoMapper.map(this)

fun ArtistEntity.toArtist(): Artist = ArtistEntityMapper.map(this)
