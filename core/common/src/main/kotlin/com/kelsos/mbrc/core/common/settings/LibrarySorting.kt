package com.kelsos.mbrc.core.common.settings

enum class SortOrder(val value: String) {
  ASC("asc"),
  DESC("desc");

  companion object {
    fun fromString(value: String): SortOrder = when (value) {
      DESC.value -> DESC
      else -> ASC
    }
  }
}

enum class GenreSortField(val value: String) {
  NAME("name");

  companion object {
    fun fromString(value: String): GenreSortField = when (value) {
      else -> NAME
    }
  }
}

enum class ArtistSortField(val value: String) {
  NAME("name");

  companion object {
    fun fromString(value: String): ArtistSortField = when (value) {
      else -> NAME
    }
  }
}

enum class AlbumSortField(val value: String) {
  NAME("name"),
  ARTIST("artist"),
  YEAR("year");

  companion object {
    fun fromString(value: String): AlbumSortField = when (value) {
      ARTIST.value -> ARTIST
      YEAR.value -> YEAR
      else -> NAME
    }
  }
}

data class SortPreference<T>(val field: T, val order: SortOrder) {
  companion object {
    fun <T> encode(preference: SortPreference<T>, fieldToString: (T) -> String): String =
      "${fieldToString(preference.field)}:${preference.order.value}"

    fun <T> decode(
      encoded: String,
      fieldFromString: (String) -> T,
      defaultField: T
    ): SortPreference<T> {
      val parts = encoded.split(":")
      return if (parts.size == 2) {
        SortPreference(
          field = fieldFromString(parts[0]),
          order = SortOrder.fromString(parts[1])
        )
      } else {
        SortPreference(field = defaultField, order = SortOrder.ASC)
      }
    }
  }
}

typealias GenreSortPreference = SortPreference<GenreSortField>
typealias ArtistSortPreference = SortPreference<ArtistSortField>
typealias AlbumSortPreference = SortPreference<AlbumSortField>
