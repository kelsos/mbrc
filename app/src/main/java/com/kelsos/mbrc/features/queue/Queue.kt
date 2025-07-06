package com.kelsos.mbrc.features.queue

sealed class Queue(val action: String) {
  object Next : Queue(NEXT)

  object Last : Queue(LAST)

  object Now : Queue(NOW)

  object AddAll : Queue(ADD_ALL)

  object PlayAlbum : Queue(PLAY_ALBUM)

  object PlayArtist : Queue(PLAY_ARTIST)

  object Default : Queue(DEFAULT)

  companion object {
    const val NEXT = "next"
    const val LAST = "last"
    const val NOW = "now"
    const val ADD_ALL = "add-all"
    const val PLAY_ALBUM = "play-album"
    const val PLAY_ARTIST = "play-artist"
    const val DEFAULT = "default"

    fun fromString(string: String): Queue = when (string) {
      NEXT -> Next
      LAST -> Last
      NOW -> Now
      ADD_ALL -> AddAll
      PLAY_ALBUM -> PlayAlbum
      PLAY_ARTIST -> PlayArtist
      DEFAULT -> Default
      else -> throw IllegalArgumentException("$string is not a recognized option")
    }
  }
}
