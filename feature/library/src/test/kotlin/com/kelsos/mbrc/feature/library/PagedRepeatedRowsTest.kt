package com.kelsos.mbrc.feature.library

import androidx.paging.PagingSource
import androidx.paging.PagingState
import androidx.paging.testing.asSnapshot
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertWithMessage
import com.kelsos.mbrc.core.data.paged
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class PagedRepeatedRowsTest {
  @Test
  fun `a row repeated across pages is dropped with placeholders off`() = runTest {
    assertRepeatDropped(enablePlaceholders = false)
  }

  @Test
  fun `a row repeated across pages is dropped with placeholders on`() = runTest {
    assertRepeatDropped(enablePlaceholders = true)
  }

  private suspend fun assertRepeatDropped(enablePlaceholders: Boolean) {
    val rows = paged(
      pagingSourceFactory = { ShiftedSecondPageSource() },
      key = { it },
      enablePlaceholders = enablePlaceholders
    ) { it }.asSnapshot { scrollTo(index = SCROLL_INTO_SECOND_PAGE) }

    assertWithMessage("each row once, in order, across the shifted page boundary")
      .that(rows)
      .containsExactlyElementsIn(0L until LAST_ROW_EXCLUSIVE)
      .inOrder()
  }

  /** Serves a second page starting one row early, as a read at a shifted offset would. */
  private class ShiftedSecondPageSource : PagingSource<Int, Long>() {
    override val jumpingSupported = false

    override fun getRefreshKey(state: PagingState<Int, Long>): Int? = null

    override suspend fun load(params: LoadParams<Int>): LoadResult<Int, Long> = when (params) {
      is LoadParams.Append -> page(
        from = FIRST_PAGE_SIZE - 1L,
        until = LAST_ROW_EXCLUSIVE,
        nextKey = null
      )

      else -> page(from = 0, until = FIRST_PAGE_SIZE.toLong(), nextKey = FIRST_PAGE_SIZE)
    }

    private fun page(from: Long, until: Long, nextKey: Int?) = LoadResult.Page(
      data = (from until until).toList(),
      prevKey = null,
      nextKey = nextKey,
      itemsBefore = from.toInt(),
      itemsAfter = (LAST_ROW_EXCLUSIVE - until).toInt()
    )
  }

  private companion object {
    const val FIRST_PAGE_SIZE = 100
    const val SCROLL_INTO_SECOND_PAGE = 120
    const val LAST_ROW_EXCLUSIVE = 150L
  }
}
