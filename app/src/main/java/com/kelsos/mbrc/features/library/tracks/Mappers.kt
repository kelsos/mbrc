package com.kelsos.mbrc.features.library.tracks

import com.kelsos.mbrc.common.data.Mapper
import java.util.regex.Pattern

object TrackDtoMapper : Mapper<TrackDto, TrackEntity> {

  private val pattern = Pattern.compile(""".*(\d{4}).*""")
  private val matcher = pattern.matcher("")

  private val parseYear: (year: String) -> String = { year ->
    with(matcher.reset(year)) {
      if (find()) {
        group(1).orEmpty()
      } else {
        ""
      }
    }
  }

  override fun map(from: TrackDto): TrackEntity = TrackEntity(
    artist = from.artist,
    title = from.title,
    src = from.src,
    trackno = from.trackno,
    disc = from.disc,
    albumArtist = from.albumArtist,
    album = from.album,
    genre = from.genre,
    year = from.year,
    sortableYear = parseYear(from.year)
  )
}

object TrackEntityMapper : Mapper<TrackEntity, Track> {
  override fun map(from: TrackEntity): Track = Track(
    artist = from.artist,
    title = from.title,
    src = from.src,
    trackno = from.trackno,
    disc = from.disc,
    albumArtist = from.albumArtist,
    album = from.album,
    genre = from.genre,
    year = from.year,
    id = from.id
  )
}

fun TrackDto.toEntity(): TrackEntity = TrackDtoMapper.map(this)

fun TrackEntity.toTrack(): Track = TrackEntityMapper.map(this)
