package com.kelsos.mbrc.features.library.genres

import com.kelsos.mbrc.common.data.Mapper

object GenreDtoMapper : Mapper<GenreDto, GenreEntity> {
  override fun map(from: GenreDto): GenreEntity = GenreEntity(from.genre)
}

object GenreEntityMapper : Mapper<GenreEntity, Genre> {
  override fun map(from: GenreEntity): Genre =
    Genre(
      genre = from.genre.orEmpty(),
      id = from.id ?: 0,
    )
}

fun GenreDto.toEntity(): GenreEntity = GenreDtoMapper.map(this)

fun GenreEntity.toGenre(): Genre = GenreEntityMapper.map(this)
