package com.kelsos.mbrc.core.networking

import com.google.common.truth.Truth.assertThat
import com.kelsos.mbrc.core.networking.client.GenericSocketMessage
import com.kelsos.mbrc.core.networking.data.DeserializationAdapter
import com.kelsos.mbrc.core.networking.protocol.base.Protocol
import com.kelsos.mbrc.core.networking.protocol.models.Page
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import java.lang.reflect.ParameterizedType
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test

/**
 * Covers how `getAllPages` decides it has read the whole collection.
 *
 * The termination condition is `page.offset + page.limit > page.total`, which trusts the server's
 * own `total`. That trust is the suspected cause of the partial now-playing loads (only ~6-7k of
 * ~15.7k tracks), so the boundaries are pinned here rather than reasoned about.
 *
 * Tests named `currently` record behaviour that is not necessarily desirable. They exist so a
 * change to it is deliberate, and should be read as a description of today rather than a
 * specification.
 */
class ApiBaseTest {

  private lateinit var requestManager: RequestManager
  private lateinit var adapter: DeserializationAdapter
  private lateinit var connection: ActiveConnection
  private lateinit var apiBase: ApiBase

  /** Pages the fake server will hand back, in order, one per request. */
  private var pages: List<Page<String>> = emptyList()
  private var requestCount = 0

  @Before
  fun setUp() {
    connection = mockk(relaxed = true)
    requestManager = mockk {
      coEvery { openConnection(any()) } returns connection
      coEvery { request(any(), any()) } coAnswers { "response-${requestCount++}" }
    }
    adapter = mockk()
    apiBase = ApiBase(adapter, requestManager)
  }

  /**
   * Serves [pages] in order, keyed off how many requests have already been answered, so the
   * sequence the production loop pulls is exactly the sequence declared by each test.
   */
  private fun serve(vararg served: Page<String>) {
    pages = served.toList()
    requestCount = 0
    every { adapter.objectify<GenericSocketMessage<Page<String>>>(any(), any<ParameterizedType>()) }
      .answers {
        val index = firstArg<String>().substringAfterLast('-').toInt()
        GenericSocketMessage("context", pages[index])
      }
  }

  private fun page(offset: Int, total: Int, size: Int, limit: Int = ApiBase.LIMIT): Page<String> =
    Page<String>().apply {
      this.offset = offset
      this.total = total
      this.limit = limit
      this.data = List(size) { "item-${offset + it}" }
    }

  @Test
  fun `a collection smaller than one page is read in a single request`() = runTest {
    serve(page(offset = 0, total = 10, size = 10))

    val emitted = apiBase.getAllPages(Protocol.LibraryBrowseTracks, String::class, null).toList()

    assertThat(emitted).hasSize(1)
    assertThat(emitted.flatten()).hasSize(10)
    coVerify(exactly = 1) { requestManager.request(any(), any()) }
  }

  @Test
  fun `an empty collection stops after one request`() = runTest {
    serve(page(offset = 0, total = 0, size = 0))

    val emitted = apiBase.getAllPages(Protocol.LibraryBrowseTracks, String::class, null).toList()

    assertThat(emitted.flatten()).isEmpty()
    coVerify(exactly = 1) { requestManager.request(any(), any()) }
  }

  @Test
  fun `a partial final page ends the read`() = runTest {
    serve(
      page(offset = 0, total = 1_000, size = ApiBase.LIMIT),
      page(offset = ApiBase.LIMIT, total = 1_000, size = 200)
    )

    val emitted = apiBase.getAllPages(Protocol.LibraryBrowseTracks, String::class, null).toList()

    assertThat(emitted.flatten()).hasSize(1_000)
    coVerify(exactly = 2) { requestManager.request(any(), any()) }
  }

  @Test
  fun `a total that is an exact multiple of the page size currently costs an extra request`() =
    runTest {
      val total = ApiBase.LIMIT * 2
      serve(
        page(offset = 0, total = total, size = ApiBase.LIMIT),
        page(offset = ApiBase.LIMIT, total = total, size = ApiBase.LIMIT),
        page(offset = total, total = total, size = 0)
      )

      val emitted = apiBase.getAllPages(Protocol.LibraryBrowseTracks, String::class, null).toList()

      assertThat(emitted.flatten()).hasSize(total)
      assertThat(emitted.last()).isEmpty()
      coVerify(exactly = 3) { requestManager.request(any(), any()) }
    }

  @Test
  fun `every item is read when the collection spans many pages`() = runTest {
    val total = ApiBase.LIMIT * 3 + 17
    serve(
      page(offset = 0, total = total, size = ApiBase.LIMIT),
      page(offset = ApiBase.LIMIT, total = total, size = ApiBase.LIMIT),
      page(offset = ApiBase.LIMIT * 2, total = total, size = ApiBase.LIMIT),
      page(offset = ApiBase.LIMIT * 3, total = total, size = 17)
    )

    val emitted = apiBase.getAllPages(Protocol.LibraryBrowseTracks, String::class, null).toList()

    assertThat(emitted.flatten()).hasSize(total)
    assertThat(emitted.flatten().distinct()).hasSize(total)
  }

  @Test
  fun `a total the server reports too low currently truncates the read`() = runTest {
    val serverUnderReportsWhileTheQueueIsStillBeingBuilt = page(offset = 0, total = 500, size = 500)
    serve(serverUnderReportsWhileTheQueueIsStillBeingBuilt)

    val emitted = apiBase.getAllPages(Protocol.LibraryBrowseTracks, String::class, null).toList()

    assertThat(emitted.flatten()).hasSize(500)
    coVerify(exactly = 1) { requestManager.request(any(), any()) }
  }

  @Test
  fun `progress reports items read against the reported total`() = runTest {
    serve(
      page(offset = 0, total = 1_000, size = ApiBase.LIMIT),
      page(offset = ApiBase.LIMIT, total = 1_000, size = 200)
    )
    val reported = mutableListOf<Pair<Int, Int>>()

    apiBase.getAllPages(Protocol.LibraryBrowseTracks, String::class) { current, total ->
      reported.add(current to total)
    }.toList()

    assertThat(reported).containsExactly(
      ApiBase.LIMIT to 1_000,
      1_000 to 1_000
    ).inOrder()
  }

  @Test
  fun `the connection is closed once the whole collection is read`() = runTest {
    serve(page(offset = 0, total = 10, size = 10))

    apiBase.getAllPages(Protocol.LibraryBrowseTracks, String::class, null).toList()

    coVerify(exactly = 1) { connection.close() }
  }

  @Test
  fun `nothing is requested until the flow is collected`() = runTest {
    serve(page(offset = 0, total = 10, size = 10))

    apiBase.getAllPages(Protocol.LibraryBrowseTracks, String::class, null)

    coVerify(exactly = 0) { requestManager.openConnection(any()) }
    coVerify(exactly = 0) { requestManager.request(any(), any()) }
  }
}
