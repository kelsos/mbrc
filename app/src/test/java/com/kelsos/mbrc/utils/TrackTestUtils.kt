package com.kelsos.mbrc.utils

import com.kelsos.mbrc.features.library.tracks.TrackEntity

/**
 * Test utility for generating TrackEntity instances with sensible defaults
 * and the ability to override specific properties.
 */
class TrackGenerator(
  private val baseArtist: String = "Artist",
  private val baseTitle: String = "Track",
  private val basePath: String = "/path/to",
  private val baseAlbum: String? = null,
  private val genre: String = "Rock",
  private val year: String = "2023",
  private val dateAdded: Long = DEFAULT_DATE_ADDED
) {
  companion object {
    const val DEFAULT_DATE_ADDED = 1000L
    const val OLDER_DATE_ADDED = 500L
  }

  /**
   * Generates multiple tracks with incremented names and track numbers.
   *
   * @param count Number of tracks to generate
   * @param configure Lambda to customize individual tracks
   * @return List of generated TrackEntity instances
   */
  fun generateTracks(
    count: Int,
    configure: (index: Int, TrackEntityBuilder) -> Unit = { _, _ -> }
  ): List<TrackEntity> = (1..count).map { index ->
    val artistName = "$baseArtist $index"
    val builder = TrackEntityBuilder(
      artist = artistName,
      title = "$baseTitle $index",
      src = "$basePath/${baseTitle.lowercase()}$index.mp3",
      trackno = index,
      album = baseAlbum ?: "Album $baseArtist $index",
      albumArtist = baseArtist,
      genre = genre,
      year = year,
      dateAdded = dateAdded
    )
    configure(index, builder)
    builder.build()
  }

  /**
   * Generates a single track with the base configuration.
   *
   * @param configure Lambda to customize the track
   * @return Generated TrackEntity instance
   */
  fun generateTrack(configure: TrackEntityBuilder.() -> Unit = {}): TrackEntity {
    val builder = TrackEntityBuilder(
      artist = baseArtist,
      title = baseTitle,
      src = "$basePath/${baseTitle.lowercase()}.mp3",
      album = baseAlbum ?: "Album $baseArtist",
      albumArtist = baseArtist,
      genre = genre,
      year = year,
      dateAdded = dateAdded
    )
    builder.configure()
    return builder.build()
  }
}

/**
 * Builder for TrackEntity with mutable properties for easy customization.
 */
class TrackEntityBuilder(
  var artist: String,
  var title: String,
  var src: String,
  var trackno: Int = 1,
  var disc: Int = 1,
  var albumArtist: String = artist,
  var album: String,
  var genre: String,
  var year: String,
  var sortableYear: String = year,
  var dateAdded: Long
) {
  fun build(): TrackEntity = TrackEntity(
    artist = artist,
    title = title,
    src = src,
    trackno = trackno,
    disc = disc,
    albumArtist = albumArtist,
    album = album,
    genre = genre,
    year = year,
    sortableYear = sortableYear,
    dateAdded = dateAdded
  )
}
