package com.kelsos.mbrc.changelog

sealed class ChangeLogEntry {
  data class Version(val release: String, val version: String) : ChangeLogEntry()

  data class Entry(val text: String, val type: EntryType) : ChangeLogEntry()
}

sealed class EntryType {
  object BUG : EntryType()

  object REMOVED : EntryType()

  object FEATURE : EntryType()
}
