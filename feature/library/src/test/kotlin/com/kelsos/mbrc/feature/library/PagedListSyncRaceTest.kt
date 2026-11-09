package com.kelsos.mbrc.feature.library

import android.content.Context
import androidx.paging.PagingData
import androidx.paging.testing.SnapshotLoader
import androidx.paging.testing.asSnapshot
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertWithMessage
import com.kelsos.mbrc.core.common.settings.SortOrder
import com.kelsos.mbrc.core.common.settings.TrackSortField
import com.kelsos.mbrc.core.common.utilities.coroutines.AppCoroutineDispatchers
import com.kelsos.mbrc.core.data.Database
import com.kelsos.mbrc.core.data.library.artist.ArtistEntity
import com.kelsos.mbrc.core.data.library.track.TrackEntity
import com.kelsos.mbrc.feature.library.artists.ArtistRepositoryImpl
import com.kelsos.mbrc.feature.library.tracks.TrackRepositoryImpl
import io.mockk.mockk
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executor
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import kotlin.concurrent.thread
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.Timeout
import org.junit.runner.RunWith

/**
 * Drives the real repository, Room paging source and Pager against a file-backed WAL database, the
 * way production runs them, so that a page read mid-sync lands at a shifted offset.
 *
 * [commitBatchWhileNextBatchHoldsTheWriter] arranges the window deterministically. The batch has
 * committed, Room's background refresh has claimed `pendingRefresh` and is waiting for the writer,
 * so the append's own `invalidationTracker.refresh()` finds nothing pending and reports no change.
 */
@RunWith(AndroidJUnit4::class)
class PagedListSyncRaceTest {
  @get:Rule
  val timeout: Timeout = Timeout.seconds(TIMEOUT_SECONDS)

  private val context: Context = ApplicationProvider.getApplicationContext()
  private val pool = Executors.newFixedThreadPool(POOL_SIZE)
  private val queryExecutor = GatedExecutor(pool)
  private val releaseWriter = CountDownLatch(1)
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
  }

  @After
  fun tearDown() {
    releaseWriter.countDown()
    queryExecutor.open()
    database.close()
    pool.shutdown()
    pool.awaitTermination(TIMEOUT_SECONDS, TimeUnit.SECONDS)
    context.deleteDatabase(DB_NAME)
  }

  @Test
  fun `tracks never repeat an id when a sync batch shifts the rows between two pages`() =
    runBlocking {
      val dao = database.trackDao()
      dao.insertAll((0 until ROW_COUNT).map { track("Track %03d".format(it)) })
      val repository = TrackRepositoryImpl(dao, mockk(), dispatchers())

      val ids = repository.getAll(TrackSortField.TITLE, SortOrder.ASC).idsAfterRacingAppend(
        id = { it.id },
        commitBatch = { dao.insertAll(listOf(track("A new track"))) }
      )

      assertNoRepeats(ids)
    }

  @Test
  fun `artists never repeat an id when a sync batch shifts the rows between two pages`() =
    runBlocking {
      val dao = database.artistDao()
      dao.insertAll((0 until ROW_COUNT).map { ArtistEntity(artist = "Artist %03d".format(it)) })
      val repository = ArtistRepositoryImpl(dao, mockk(), dispatchers())

      val ids = repository.getAll(SortOrder.ASC).idsAfterRacingAppend(
        id = { it.id },
        commitBatch = { dao.insertAll(listOf(ArtistEntity(artist = "A new artist"))) }
      )

      assertNoRepeats(ids)
    }

  @Test
  fun `the row a repeat hid comes back once the sync lets the invalidation through`() =
    runBlocking {
      val dao = database.trackDao()
      dao.insertAll((0 until ROW_COUNT).map { track("Track %03d".format(it)) })
      val repository = TrackRepositoryImpl(dao, mockk(), dispatchers())

      val titles = repository.getAll(TrackSortField.TITLE, SortOrder.ASC).asSnapshot {
        settleInitialLoad()
        commitBatchWhileNextBatchHoldsTheWriter { dao.insertAll(listOf(track("A new track"))) }
        scrollTo(index = SCROLL_PAST_FIRST_PAGE)
        releaseWriter.countDown()
        delay(SETTLE_MILLIS)
        prependScrollWhile { true }
      }.map { it.title }

      assertWithMessage("the refreshed list starts with the row the sync inserted")
        .that(titles.first())
        .isEqualTo("A new track")
      assertWithMessage("the refreshed list holds each row once")
        .that(titles)
        .containsNoDuplicates()
    }

  private suspend fun <T : Any> Flow<PagingData<T>>.idsAfterRacingAppend(
    id: (T) -> Long,
    commitBatch: () -> Unit
  ): List<Long> {
    val snapshot = asSnapshot {
      settleInitialLoad()
      commitBatchWhileNextBatchHoldsTheWriter(commitBatch)
      scrollTo(index = SCROLL_PAST_FIRST_PAGE)
    }
    return snapshot.map(id)
  }

  private suspend fun SnapshotLoader<*>.settleInitialLoad() {
    scrollTo(index = 0)
    Thread.sleep(SETTLE_MILLIS)
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

  private fun assertNoRepeats(ids: List<Long>) {
    assertWithMessage("loaded past the first page, otherwise nothing could repeat")
      .that(ids.size)
      .isGreaterThan(INITIAL_LOAD_SIZE)
    assertWithMessage("ids a LazyColumn would be handed as keys")
      .that(ids)
      .containsNoDuplicates()
  }

  private fun track(title: String) = TrackEntity(
    artist = "Artist",
    title = title,
    src = "/music/$title.mp3",
    trackno = 1,
    disc = 1,
    albumArtist = "Artist",
    album = "Album",
    genre = "Rock",
    year = "2026",
    sortableYear = "2026"
  )

  private fun dispatchers() = mockk<AppCoroutineDispatchers>()

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
    const val POOL_SIZE = 4
    const val ROW_COUNT = 300
    const val INITIAL_LOAD_SIZE = 100
    const val SCROLL_PAST_FIRST_PAGE = 120
    const val SETTLE_MILLIS = 300L
    const val TIMEOUT_SECONDS = 60L
  }
}
