package com.kelsos.mbrc.core.common.utilities

/**
 * Splits a MusicBee multi-value tag into its individual values.
 *
 * MusicBee documents artist, composer, genre, performer, guest artist and
 * remixer as multi-value; album artist is NOT one of them and must never be
 * split. A literal ';' inside a name is not expected either - MusicBee tells
 * users to substitute the Lisu tone character 'ꓼ' in that case.
 *
 * MusicBee is inconsistent about whitespace around the separator: genre arrives
 * as "Rock; Electronic" while a multi-artist field arrives as "Artist1;Artist2".
 * We therefore split on ';' and trim each fragment rather than splitting on a
 * fixed "; " literal.
 *
 * Empty fragments and duplicate values are dropped; the original order is kept.
 */
fun splitTags(raw: String): List<String> = raw.split(';')
  .map { it.trim() }
  .filter { it.isNotEmpty() }
  .distinct()

/**
 * Folds a tag name the way SQLite's `NOCASE` collation does, for keying lookups against rows stored
 * in a `COLLATE NOCASE` column.
 *
 * Deliberately ASCII-only, because `NOCASE` is. [String.lowercase] is Unicode-aware and would treat
 * names SQLite keeps apart as equal, collapsing two real rows onto one key and silently losing the
 * other one's id. Matching SQLite exactly, even where that means folding less, is what keeps a
 * lookup and the row it is meant to find in agreement.
 */
fun foldForTagIndex(name: String): String = buildString(name.length) {
  for (character in name) {
    append(if (character in 'A'..'Z') character + ASCII_CASE_OFFSET else character)
  }
}

private const val ASCII_CASE_OFFSET = 32
