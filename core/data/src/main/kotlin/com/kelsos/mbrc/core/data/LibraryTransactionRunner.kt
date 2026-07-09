package com.kelsos.mbrc.core.data

import androidx.room.withTransaction

/**
 * Runs a block inside a single Room transaction. Lives in core/data so callers in
 * feature modules (which only see this module via `implementation`) can wrap
 * multi-DAO work atomically without depending on Room directly.
 */
class LibraryTransactionRunner(private val db: Database) {
  suspend fun <R> immediate(block: suspend () -> R): R = db.withTransaction { block() }
}
