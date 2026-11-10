package com.kelsos.mbrc.core.data

import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.PagingSource
import androidx.paging.filter
import androidx.paging.map
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/** Rows in the first page of every [paged] list; later pages hold half as many. */
const val PAGED_INITIAL_LOAD_SIZE = 100

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
 * Room has no synchronous hook to close that gap; doing so here would mean every writer bumping a
 * shared generation that a wrapping source checks. A repeat is dropped instead. Usually that is
 * short-lived: the refresh still completes and invalidates the source, and the next generation
 * starts clean, which is why the seen keys live per [PagingData]. Room also ignores invalidations
 * that arrive before a source's first load returns, so a write landing then leaves the shifted
 * pages in place until the next write. Dropping repeats also relies on the config's unbounded
 * `maxSize`: a page dropped and reloaded on scroll-back would otherwise be filtered out entirely.
 *
 * @param key the row's identity, the same value its list renders with. Rows with a different
 * identity must never share one, or the later row disappears from the list.
 */
fun <T : Any, I : Any> paged(
  pagingSourceFactory: () -> PagingSource<Int, T>,
  key: (I) -> Any,
  enablePlaceholders: Boolean = false,
  transform: (value: T) -> I
): Flow<PagingData<I>> {
  val config =
    PagingConfig(
      enablePlaceholders = enablePlaceholders,
      pageSize = PAGED_INITIAL_LOAD_SIZE / 2,
      prefetchDistance = PAGED_INITIAL_LOAD_SIZE / 4,
      initialLoadSize = PAGED_INITIAL_LOAD_SIZE
    )
  return Pager(
    config,
    pagingSourceFactory = pagingSourceFactory
  ).flow.map { data ->
    val seen = HashSet<Any>()
    data.map(transform).filter { seen.add(key(it)) }
  }
}
