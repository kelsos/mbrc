package com.kelsos.mbrc.feature.library

import android.content.Context
import androidx.paging.testing.SnapshotLoader
import androidx.paging.testing.asSnapshot
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertWithMessage
import com.kelsos.mbrc.core.common.settings.SortOrder
import com.kelsos.mbrc.core.common.settings.TrackSortField
import com.kelsos.mbrc.core.common.test.coroutineTestTimeout
import com.kelsos.mbrc.core.data.Database
import com.kelsos.mbrc.core.data.PAGED_INITIAL_LOAD_SIZE
import com.kelsos.mbrc.core.data.library.track.Track
import com.kelsos.mbrc.core.data.test.TrackGenerator
import com.kelsos.mbrc.feature.library.tracks.TrackRepositoryImpl
import io.mockk.mockk
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executor
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import kotlin.concurrent.thread
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.Timeout
import org.junit.runner.RunWith

/**
 * Drives the real repository, Room paging source and Pager against a file-backed WAL database, the
 * way production runs them, so that a page read mid-sync lands at a shifted offset. Every paged list
 * shares this path through `paged()`, so the tracks list stands in for all of them.
 *
 * [commitBatchWhileNextBatchHoldsTheWriter] arranges the window. The batch has committed, Room's
 * background refresh has claimed `pendingRefresh` and is waiting for the writer, so the append's own
 * `invalidationTracker.refresh()` finds nothing pending and reports no change.
 */
@RunWith(AndroidJUnit4::class)
class PagedListSyncRaceTest {
  @get:Rule
  val timeout: Timeout = coroutineTestTimeout()

  private val context: Context = ApplicationProvider.getApplicationContext()
  private val pool = Executors.newFixedThreadPool(POOL_SIZE)
  private val queryExecutor = GatedExecutor(pool)
  private val releaseWriter = CountDownLatch(1)
  private val tracks = TrackGenerator(basePath = "/music")
  private lateinit var database: Database

  @Before
  fun setUp() {
    context.deleteDatabase(DB_NAME)
    database = Room
      .databaseBuilder(context, Database::class.java, DB_NAME)
      .setJournalMode(RoomDatabase.JournalMode.WRITE_AHEAD_LOGGING)
      .setQueryExecutor(queryExecutor)
      .allowMainThreadQueries()
      .build()
    database.trackDao().insertAll((0 until ROW_COUNT).map { track("Track %03d".format(it)) })
  }

  @After
  fun tearDown() {
    releaseWriter.countDown()
    queryExecutor.open()
    database.close()
    pool.shutdown()
    pool.awaitTermination(SHUTDOWN_SECONDS, TimeUnit.SECONDS)
    context.deleteDatabase(DB_NAME)
  }

  @Test
  fun `no id repeats when a sync batch shifts the rows between two pages`() = runBlocking {
    val tracks = racedSnapshot()

    assertWithMessage("loaded past the first page, otherwise nothing could repeat")
      .that(tracks.size)
      .isGreaterThan(PAGED_INITIAL_LOAD_SIZE)
    assertWithMessage("ids a LazyColumn would be handed as keys")
      .that(tracks.map { it.id })
      .containsNoDuplicates()
  }

  @Test
  fun `the row a repeat hid comes back once the sync lets the invalidation through`() =
    runBlocking {
      val titles = racedSnapshot {
        releaseWriter.countDown()
        delay(SETTLE_MILLIS)
        prependScrollWhile { true }
      }.map { it.title }

      assertWithMessage("the refreshed list starts with the row the sync inserted")
        .that(titles.first())
        .isEqualTo(INSERTED_TITLE)
      assertWithMessage("the refreshed list holds each row once")
        .that(titles)
        .containsNoDuplicates()
    }

  private suspend fun racedSnapshot(
    afterRace: suspend SnapshotLoader<Track>.() -> Unit = {}
  ): List<Track> {
    val dao = database.trackDao()
    val repository = TrackRepositoryImpl(dao, mockk(), mockk())
    return repository.getAll(TrackSortField.TITLE, SortOrder.ASC).asSnapshot {
      scrollTo(index = 0)
      Thread.sleep(SETTLE_MILLIS)
      commitBatchWhileNextBatchHoldsTheWriter { dao.insertAll(listOf(track(INSERTED_TITLE))) }
      scrollTo(index = PAGED_INITIAL_LOAD_SIZE + SCROLL_PAST_BOUNDARY)
      afterRace()
    }
  }

  private fun commitBatchWhileNextBatchHoldsTheWriter(commitBatch: () -> Unit) {
    queryExecutor.close()
    commitBatch()

    val writerHeld = CountDownLatch(1)
    thread(name = "next-sync-batch") {
      database.runInTransaction {
        writerHeld.countDown()
        releaseWriter.await()
      }
    }
    writerHeld.await()

    queryExecutor.open()
    Thread.sleep(SETTLE_MILLIS)
  }

  private fun track(title: String) = tracks.generateTrack {
    this.title = title
    src = "/music/$title.mp3"
  }

  /** Queues work while closed, so a test decides when Room's background refresh may start. */
  private class GatedExecutor(private val delegate: Executor) : Executor {
    private val held = ConcurrentLinkedQueue<Runnable>()

    @Volatile
    private var closed = false

    override fun execute(command: Runnable) {
      if (closed) held.add(command) else delegate.execute(command)
    }

    fun close() {
      closed = true
    }

    fun open() {
      closed = false
      generateSequence { held.poll() }.forEach(delegate::execute)
    }
  }

  private companion object {
    const val DB_NAME = "paged-list-sync-race.db"
    const val INSERTED_TITLE = "A new track"
    const val POOL_SIZE = 4
    const val ROW_COUNT = 300
    const val SCROLL_PAST_BOUNDARY = 20
    const val SETTLE_MILLIS = 300L
    const val SHUTDOWN_SECONDS = 10L
  }
}
