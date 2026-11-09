package com.kelsos.mbrc.core.data

import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.PagingSource
import androidx.paging.filter
import androidx.paging.map
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * Pages a Room query, guaranteeing that [key] never repeats within one generation of the list.
 *
 * Room pages with `LIMIT`/`OFFSET` and has one chance to notice that a write moved the rows under a
 * loaded page: `LimitOffsetPagingSource` asks `InvalidationTracker.refresh()` after each read. That
 * call reports no change once the tracker's background refresh has claimed the pending flag, even
 * though the refresh has not yet invalidated the source. A read landing in that gap comes back as
 * a valid page at a shifted offset, so a row inserted above a page boundary pushes the boundary row
 * onto the next page as well. Two slots then share a `LazyColumn` key, which is a fatal
 * `IllegalArgumentException`.
 *
 * Nothing outside Room can close that gap, so a repeat is dropped here instead. It is short-lived:
 * the refresh still completes and invalidates the source, and the next generation starts clean,
 * which is why the seen keys live per [PagingData].
 *
 * @param key the row's identity, which must match the key its list renders with. Rows with a
 * different identity must never share one, or the later row disappears from the list.
 */
fun <T : Any, I : Any> paged(
  pagingSourceFactory: () -> PagingSource<Int, T>,
  key: (T) -> Any,
  enablePlaceholders: Boolean = false,
  transform: (value: T) -> I
): Flow<PagingData<I>> {
  val config =
    PagingConfig(
      enablePlaceholders = enablePlaceholders,
      pageSize = 50,
      prefetchDistance = 25,
      initialLoadSize = 100
    )
  return Pager(
    config,
    pagingSourceFactory = pagingSourceFactory
  ).flow.map { data ->
    val seen = HashSet<Any>()
    data.filter { seen.add(key(it)) }.map(transform)
  }
}
