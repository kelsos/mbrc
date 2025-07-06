package com.kelsos.mbrc.features.library.tracks

import com.kelsos.mbrc.common.data.Mapper

object TrackDtoMapper : Mapper<TrackDto, TrackEntity> {
  override fun map(from: TrackDto): TrackEntity = TrackEntity(
    from.artist,
    from.title,
    from.src,
    from.trackno,
    from.disc,
    from.albumArtist,
    from.album,
    from.genre
  )
}

object TrackEntityMapper : Mapper<TrackEntity, Track> {
  override fun map(from: TrackEntity): Track = Track(
    artist = from.artist.orEmpty(),
    title = from.title.orEmpty(),
    src = from.src.orEmpty(),
    trackno = from.trackno ?: 0,
    disc = from.disc ?: 0,
    albumArtist = from.albumArtist.orEmpty(),
    album = from.album.orEmpty(),
    genre = from.genre.orEmpty(),
    year = "",
    id = from.id ?: 0
  )
}

fun TrackDto.toEntity(): TrackEntity = TrackDtoMapper.map(this)

fun TrackEntity.toTrack(): Track = TrackEntityMapper.map(this)
