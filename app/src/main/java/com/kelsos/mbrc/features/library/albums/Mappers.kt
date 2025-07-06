package com.kelsos.mbrc.features.library.albums

import com.kelsos.mbrc.common.data.Mapper

object AlbumDtoMapper : Mapper<AlbumDto, AlbumEntity> {
  override fun map(from: AlbumDto): AlbumEntity = AlbumEntity(
    artist = from.artist,
    album = from.album
  )
}

object AlbumEntityMapper : Mapper<AlbumEntity, Album> {
  override fun map(from: AlbumEntity): Album = Album(
    id = from.id ?: 0,
    artist = from.artist.orEmpty(),
    album = from.album.orEmpty(),
    cover = from.cover.orEmpty()
  )
}

fun AlbumEntity.toAlbum(): Album = AlbumEntityMapper.map(this)

fun AlbumDto.toEntity(): AlbumEntity = AlbumDtoMapper.map(this)
