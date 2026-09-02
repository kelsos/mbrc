package com.kelsos.mbrc.feature.playback.nowplaying

import androidx.paging.testing.asSnapshot
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import com.kelsos.mbrc.core.common.data.Progress
import com.kelsos.mbrc.core.common.test.coroutineTestTimeout
import com.kelsos.mbrc.core.common.test.testDispatcher
import com.kelsos.mbrc.core.common.test.testDispatcherModule
import com.kelsos.mbrc.core.data.Database
import com.kelsos.mbrc.core.data.nowplaying.NowPlayingDao
import com.kelsos.mbrc.core.data.nowplaying.NowPlayingEntity
import com.kelsos.mbrc.core.networking.api.PlaybackApi
import com.kelsos.mbrc.core.networking.dto.NowPlayingDto
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.Timeout
import org.junit.runner.RunWith
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.core.module.dsl.bind
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.module
import org.koin.test.KoinTest
import org.koin.test.inject

@RunWith(AndroidJUnit4::class)
class NowPlayingRepositoryTest : KoinTest {
  @get:Rule
  val timeout: Timeout = coroutineTestTimeout()

  private val testModule =
    module {
      single<PlaybackApi> { mockk(relaxed = true) }
      single {
        Room
          .inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            Database::class.java
          ).allowMainThreadQueries()
          .build()
      }
      single { get<Database>().nowPlayingDao() }
      singleOf(::NowPlayingRepositoryImpl) {
        bind<NowPlayingRepository>()
      }
    }

  private val db: Database by inject()
  private val dao: NowPlayingDao by inject()
  private val playbackApi: PlaybackApi by inject()

  private val repository: NowPlayingRepository by inject()

  val fakeAlbumQueue =
    listOf(
      NowPlayingEntity(
        title = "Midnight Dreams",
        artist = "The Electric Horizon",
        path = "/music/electric_horizon/midnight_dreams/01_midnight_dreams.mp3",
        position = 1,
        dateAdded = System.currentTimeMillis() - 3600000, // 1 hour ago
        id = 1
      ),
      NowPlayingEntity(
        title = "Neon Lights",
        artist = "The Electric Horizon",
        path = "/music/electric_horizon/midnight_dreams/02_neon_lights.mp3",
        position = 2,
        dateAdded = System.currentTimeMillis() - 3590000,
        id = 2
      ),
      NowPlayingEntity(
        title = "Digital Rain",
        artist = "The Electric Horizon",
        path = "/music/electric_horizon/midnight_dreams/03_digital_rain.mp3",
        position = 3,
        dateAdded = System.currentTimeMillis() - 3580000,
        id = 3
      ),
      NowPlayingEntity(
        title = "Lost in the City",
        artist = "The Electric Horizon",
        path = "/music/electric_horizon/midnight_dreams/04_lost_in_the_city.mp3",
        position = 4,
        dateAdded = System.currentTimeMillis() - 3570000,
        id = 4
      ),
      NowPlayingEntity(
        title = "Synth Wave",
        artist = "The Electric Horizon",
        path = "/music/electric_horizon/midnight_dreams/05_synth_wave.mp3",
        position = 5,
        dateAdded = System.currentTimeMillis() - 3560000,
        id = 5
      ),
      NowPlayingEntity(
        title = "Circuit Breaker",
        artist = "The Electric Horizon",
        path = "/music/electric_horizon/midnight_dreams/06_circuit_breaker.mp3",
        position = 6,
        dateAdded = System.currentTimeMillis() - 3550000,
        id = 6
      ),
      NowPlayingEntity(
        title = "Electric Pulse",
        artist = "The Electric Horizon",
        path = "/music/electric_horizon/midnight_dreams/07_electric_pulse.mp3",
        position = 7,
        dateAdded = System.currentTimeMillis() - 3540000,
        id = 7
      ),
      NowPlayingEntity(
        title = "Data Stream",
        artist = "The Electric Horizon",
        path = "/music/electric_horizon/midnight_dreams/08_data_stream.mp3",
        position = 8,
        dateAdded = System.currentTimeMillis() - 3530000,
        id = 8
      ),
      NowPlayingEntity(
        title = "Virtual Reality",
        artist = "The Electric Horizon",
        path = "/music/electric_horizon/midnight_dreams/09_virtual_reality.mp3",
        position = 9,
        dateAdded = System.currentTimeMillis() - 3520000,
        id = 9
      ),
      NowPlayingEntity(
        title = "Dawn Protocol",
        artist = "The Electric Horizon",
        path = "/music/electric_horizon/midnight_dreams/10_dawn_protocol.mp3",
        position = 10,
        dateAdded = System.currentTimeMillis() - 3510000,
        id = 10
      )
    )

  @Before
  fun setUp() {
    startKoin { modules(listOf(testModule, testDispatcherModule)) }
    dao.deleteAll()
    dao.insertAll(fakeAlbumQueue)
  }

  @After
  fun tearDown() {
    db.close()
    stopKoin()
  }

  @Test
  fun moveCallsDaoMoveFrom2To5() {
    runTest(testDispatcher) {
      val from = 2
      val to = 5
      val preMoveAlbumQueue = dao.all()
      assertThat(preMoveAlbumQueue).containsExactlyElementsIn(fakeAlbumQueue)
      val originalItem = preMoveAlbumQueue.find { it.position == from }

      repository.move(from, to)

      val postMoveAlbumQueue = dao.all().sortedBy { it.position }

      // Verify positions are still 1-10
      assertThat(
        postMoveAlbumQueue.map {
          it.position
        }
      ).containsExactly(1, 2, 3, 4, 5, 6, 7, 8, 9, 10).inOrder()

      // Verify the moved item is now at position 5
      val movedItem = postMoveAlbumQueue.find { it.id == originalItem?.id }
      assertThat(movedItem?.position).isEqualTo(to)

      // Verify the new order of IDs by position:
      // Position 1: ID 1 (unchanged)
      // Position 2: ID 3 (was 3rd, now 2nd)
      // Position 3: ID 4 (was 4th, now 3rd)
      // Position 4: ID 5 (was 5th, now 4th)
      // Position 5: ID 2 (was 2nd, now 5th)
      // Positions 6-10: unchanged
      val expectedIdOrder = listOf(1L, 3L, 4L, 5L, 2L, 6L, 7L, 8L, 9L, 10L)
      assertThat(
        postMoveAlbumQueue.map {
          it.id
        }
      ).containsExactlyElementsIn(expectedIdOrder).inOrder()
    }
  }

  @Test
  fun moveCallsDaoMoveFrom8To4() {
    runTest(testDispatcher) {
      val from = 8
      val to = 4
      val preMoveAlbumQueue = dao.all()
      assertThat(preMoveAlbumQueue).containsExactlyElementsIn(fakeAlbumQueue)
      val originalItem = preMoveAlbumQueue.find { it.position == from }

      repository.move(from, to)

      val postMoveAlbumQueue = dao.all().sortedBy { it.position }

      // Verify positions are still 1-10
      assertThat(
        postMoveAlbumQueue.map {
          it.position
        }
      ).containsExactly(1, 2, 3, 4, 5, 6, 7, 8, 9, 10).inOrder()

      // Verify the moved item is now at position 4
      val movedItem = postMoveAlbumQueue.find { it.id == originalItem?.id }
      assertThat(movedItem?.position).isEqualTo(to)

      // Verify the new order of IDs by position:
      // Position 1: ID 1 (unchanged)
      // Position 2: ID 2 (unchanged)
      // Position 3: ID 3 (unchanged)
      // Position 4: ID 8 (was 8th, now 4th)
      // Position 5: ID 4 (was 4th, now 5th)
      // Position 6: ID 5 (was 5th, now 6th)
      // Position 7: ID 6 (was 6th, now 7th)
      // Position 8: ID 7 (was 7th, now 8th)
      // Positions 9-10: unchanged
      val expectedIdOrder = listOf(1L, 2L, 3L, 8L, 4L, 5L, 6L, 7L, 9L, 10L)
      assertThat(
        postMoveAlbumQueue.map {
          it.id
        }
      ).containsExactlyElementsIn(expectedIdOrder).inOrder()
    }
  }

  @Test
  fun moveWithSameFromAndToNeverAdjustsPosition() {
    runTest(testDispatcher) {
      val position = 3
      val originalItem = dao.all().find { it.position == position }

      repository.move(position, position)

      val item = dao.all().find { it.id == originalItem?.id }
      assertThat(item?.position).isEqualTo(position)
    }
  }

  @Test
  fun moveHandlesBoundaryPositions() {
    runTest(testDispatcher) {
      val originalCount = dao.count()

      repository.move(0, 99)

      assertThat(dao.count()).isEqualTo(originalCount)
    }
  }

  @Test
  fun removeCallsDaoRemoveWithCorrectPosition() {
    runTest(testDispatcher) {
      val position = 4
      val originalCount = dao.count()
      val itemToRemove = dao.all().find { it.position == position }
      val itemsAfterPosition = dao.all().filter { it.position > position }

      repository.remove(position)

      val afterRemoval = dao.all().sortedBy { it.position }

      // Verify count decreased by 1
      assertThat(dao.count()).isEqualTo(originalCount - 1)

      // Verify the specific item was removed (not just that position 4 is empty)
      assertThat(afterRemoval.none { it.id == itemToRemove?.id }).isTrue()

      // Verify positions are consecutive 1 through (originalCount-1)
      assertThat(
        afterRemoval.map {
          it.position
        }
      ).containsExactly(1, 2, 3, 4, 5, 6, 7, 8, 9).inOrder()

      // Verify items after the removed position shifted down correctly
      itemsAfterPosition.forEach { originalItem ->
        val updatedItem = dao.all().find { it.id == originalItem.id }
        assertThat(updatedItem?.position).isEqualTo(originalItem.position - 1)
      }
    }
  }

  @Test
  fun removeWithZeroPositionCallsDao() {
    runTest(testDispatcher) {
      val originalCount = dao.count()

      repository.remove(0)

      assertThat(dao.count()).isEqualTo(originalCount)
    }
  }

  @Test
  fun findPositionReturnsDaoResultWhenFound() {
    runTest(testDispatcher) {
      val query = "Data Stream"
      val expectedPosition = 8

      val result = repository.findPosition(query)

      assertThat(result).isEqualTo(expectedPosition)
    }
  }

  @Test
  fun findPositionReturnsNegativeOneWhenDaoReturnsNull() {
    runTest(testDispatcher) {
      val query = "nonexistent song"

      val result = repository.findPosition(query)

      assertThat(result).isEqualTo(-1)
    }
  }

  @Test
  fun findPositionHandlesEmptyQuery() {
    runTest(testDispatcher) {
      val query = ""

      val result = repository.findPosition(query)

      assertThat(result).isEqualTo(-1)
    }
  }

  @Test
  fun moveOperationsAreExecutedOnDatabaseDispatcher() {
    runTest(testDispatcher) {
      val originalItem = dao.all().find { it.position == 1 }

      repository.move(1, 5)

      val movedItem = dao.all().find { it.id == originalItem?.id }
      assertThat(movedItem?.position).isEqualTo(5)
    }
  }

  @Test
  fun removeOperationsAreExecutedOnDatabaseDispatcher() {
    runTest(testDispatcher) {
      val position = 3
      val originalCount = dao.count()
      val itemToRemove = dao.all().find { it.position == position }
      val itemsAfterPosition = dao.all().filter { it.position > position }

      repository.remove(position)

      val afterRemoval = dao.all().sortedBy { it.position }

      // Verify count decreased by 1
      assertThat(dao.count()).isEqualTo(originalCount - 1)

      // Verify the specific item was removed (not just that position 3 is empty)
      assertThat(afterRemoval.none { it.id == itemToRemove?.id }).isTrue()

      // Verify positions are consecutive 1 through (originalCount-1)
      assertThat(
        afterRemoval.map {
          it.position
        }
      ).containsExactly(1, 2, 3, 4, 5, 6, 7, 8, 9).inOrder()

      // Verify items after the removed position shifted down correctly
      itemsAfterPosition.forEach { originalItem ->
        val updatedItem = dao.all().find { it.id == originalItem.id }
        assertThat(updatedItem?.position).isEqualTo(originalItem.position - 1)
      }
    }
  }

  @Test
  fun findPositionOperationsAreExecutedOnDatabaseDispatcher() {
    runTest(testDispatcher) {
      val query = "Electric"

      val result = repository.findPosition(query)

      assertThat(result).isGreaterThan(0)
    }
  }

  @Test
  fun countReturnsDaoCount() {
    runTest(testDispatcher) {
      val expectedCount = dao.count()

      val result = repository.count()

      assertThat(result).isEqualTo(expectedCount)
      assertThat(result).isEqualTo(10)
    }
  }

  @Test
  fun getByIdReturnsMappedEntityWhenFound() {
    runTest(testDispatcher) {
      val expectedId = 5L
      val expectedEntity = fakeAlbumQueue.find { it.id == expectedId }

      val result = repository.getById(expectedId)

      assertThat(result).isNotNull()
      assertThat(result!!.title).isEqualTo(expectedEntity!!.title)
      assertThat(result.artist).isEqualTo(expectedEntity.artist)
      assertThat(result.path).isEqualTo(expectedEntity.path)
      assertThat(result.position).isEqualTo(expectedEntity.position)
      assertThat(result.id).isEqualTo(expectedEntity.id)
    }
  }

  @Test
  fun getByIdReturnsNullWhenNotFound() {
    runTest(testDispatcher) {
      val nonExistentId = 999L

      val result = repository.getById(nonExistentId)

      assertThat(result).isNull()
    }
  }

  @Test
  fun getRemoteFetchesFromApiAndUpdatesDatabase() {
    runTest(testDispatcher) {
      val remoteData =
        listOf(
          NowPlayingDto(
            title = "Remote Song 1",
            artist = "Remote Artist 1",
            path = "/remote/path1.mp3",
            position = 1
          ),
          NowPlayingDto(
            title = "Remote Song 2",
            artist = "Remote Artist 2",
            path = "/remote/path2.mp3",
            position = 2
          )
        )

      every { playbackApi.getNowPlayingList(any()) } returns flowOf(remoteData)

      dao.deleteAll()
      val initialCount = dao.count()

      repository.getRemote(null)

      val finalCount = dao.count()
      val allItems = dao.all()

      assertThat(finalCount).isGreaterThan(initialCount)
      assertThat(allItems).hasSize(2)
      assertThat(allItems.map { it.title }).containsExactly("Remote Song 1", "Remote Song 2")
    }
  }

  @Test
  fun getRemotePreservesIdsForUnchangedEntriesAcrossPages() {
    runTest(testDispatcher) {
      // Two pages whose (path, position) match the pre-seeded queue, so every row is an update
      // that must keep its existing primary key id (UI identity), not a fresh insert.
      val pageOne =
        fakeAlbumQueue.take(5).map {
          NowPlayingDto(
            title = it.title,
            artist = it.artist,
            path = it.path,
            position = it.position
          )
        }
      val pageTwo =
        fakeAlbumQueue.drop(5).map {
          NowPlayingDto(
            title = it.title,
            artist = it.artist,
            path = it.path,
            position = it.position
          )
        }
      every { playbackApi.getNowPlayingList(any()) } returns flowOf(pageOne, pageTwo)

      repository.getRemote(null)

      val resynced = dao.all().sortedBy { it.position }
      assertThat(resynced.map { it.position }).containsExactly(
        1, 2, 3, 4, 5, 6, 7, 8, 9, 10
      ).inOrder()
      // ids stay aligned to their original (path, position) — no wipe-and-reinsert
      assertThat(resynced.map { it.id }).containsExactly(
        1L, 2L, 3L, 4L, 5L, 6L, 7L, 8L, 9L, 10L
      ).inOrder()
      assertThat(dao.count()).isEqualTo(10)
    }
  }

  @Test
  fun getRemoteInsertsNewEntriesAndDropsStaleOnes() {
    runTest(testDispatcher) {
      // Position 1 keeps its path (update, id preserved); positions 2-3 are different paths
      // (new rows); the rest of the original queue is stale and must be removed on completion.
      val remoteData =
        listOf(
          NowPlayingDto(
            title = fakeAlbumQueue[0].title,
            artist = fakeAlbumQueue[0].artist,
            path = fakeAlbumQueue[0].path,
            position = 1
          ),
          NowPlayingDto(
            title = "Fresh A",
            artist = "Artist A",
            path = "/remote/a.mp3",
            position = 2
          ),
          NowPlayingDto(
            title = "Fresh B",
            artist = "Artist B",
            path = "/remote/b.mp3",
            position = 3
          )
        )
      every { playbackApi.getNowPlayingList(any()) } returns flowOf(remoteData)

      repository.getRemote(null)

      val resynced = dao.all().sortedBy { it.position }
      assertThat(resynced.map { it.position }).containsExactly(1, 2, 3).inOrder()
      assertThat(resynced.first { it.position == 1 }.id).isEqualTo(1L)
      assertThat(resynced.map { it.title }).containsExactly(
        fakeAlbumQueue[0].title,
        "Fresh A",
        "Fresh B"
      ).inOrder()
    }
  }

  @Test
  fun cachedRangeReadsOnlyRequestedPositionsNotWholeTable() {
    // The whole point of the fix: reconciliation must read back at most one page worth of rows,
    // never the entire queue. cachedRange is keyed on the page's exact positions (IN-list), so a
    // request for a few positions returns only those, regardless of how large the table is.
    val cached = dao.cachedRange(listOf(1, 5, 10))

    assertThat(cached.map { it.position }).containsExactly(1, 5, 10)
    assertThat(cached.map { it.id }).containsExactly(1L, 5L, 10L)
  }

  @Test
  fun getRemoteHandlesEmptyPageWithoutError() {
    runTest(testDispatcher) {
      // An empty page must not crash the reconcile (the IN-list / min lookups would otherwise
      // break on an empty position set). It should simply contribute nothing to the resync.
      val page =
        fakeAlbumQueue.take(3).map {
          NowPlayingDto(
            title = it.title,
            artist = it.artist,
            path = it.path,
            position = it.position
          )
        }
      every { playbackApi.getNowPlayingList(any()) } returns flowOf(emptyList(), page, emptyList())

      repository.getRemote(null)

      val resynced = dao.all().sortedBy { it.position }
      assertThat(resynced.map { it.position }).containsExactly(1, 2, 3).inOrder()
      assertThat(resynced.map { it.id }).containsExactly(1L, 2L, 3L).inOrder()
    }
  }

  @Test
  fun getRemoteCancelsInflightRefreshWhenANewerOneStarts() {
    runTest(testDispatcher) {
      // First refresh blocks before emitting any page, simulating one still in flight.
      val blocker = CompletableDeferred<Unit>()
      val slowFlow =
        flow {
          blocker.await()
          emit(emptyList<NowPlayingDto>())
        }
      // Second (newer) refresh carries a different queue and completes normally.
      val newQueue =
        listOf(
          NowPlayingDto(title = "B1", artist = "Artist B", path = "/remote/b1.mp3", position = 1),
          NowPlayingDto(title = "B2", artist = "Artist B", path = "/remote/b2.mp3", position = 2)
        )
      every { playbackApi.getNowPlayingList(any()) } returnsMany
        listOf(slowFlow, flowOf(newQueue))

      dao.deleteAll()

      // Start the slow refresh; it parks on the blocker and never finishes on its own.
      val inflight = launch { repository.getRemote(null) }
      advanceUntilIdle()

      // A newer trigger must cancel the in-flight one and restart from scratch.
      repository.getRemote(null)

      assertThat(inflight.isCancelled).isTrue()
      val resynced = dao.all().sortedBy { it.position }
      assertThat(resynced.map { it.title }).containsExactly("B1", "B2").inOrder()
      assertThat(dao.count()).isEqualTo(2)
    }
  }

  @Test
  fun reconcilePageReplacesTrackAtExistingPositionInPlaceWithoutLeavingDuplicate() {
    // Regression for the duplicate-key crash: changing the track at an existing position must update
    // that row in place (one row per position), not insert a second row alongside the stale one.
    val replacement =
      listOf(1, 2, 3).map { pos ->
        NowPlayingEntity(
          title = "Replacement $pos",
          artist = "New Artist",
          path = "/remote/replacement_$pos.mp3",
          position = pos,
          dateAdded = 999L
        )
      }

    dao.reconcilePage(replacement)

    // Exactly one row per affected position — no stale duplicates left behind by the page reconcile.
    val rows = dao.all().sortedBy { it.position }
    assertThat(rows.count { it.position == 1 }).isEqualTo(1)
    assertThat(rows.count { it.position == 2 }).isEqualTo(1)
    assertThat(rows.count { it.position == 3 }).isEqualTo(1)
    // Slots 1-3 kept their original ids (updated in place), so UI identity is stable.
    assertThat(rows.first { it.position == 1 }.id).isEqualTo(1L)
    assertThat(rows.first { it.position == 2 }.id).isEqualTo(2L)
    assertThat(rows.first { it.position == 3 }.id).isEqualTo(3L)
    assertThat(rows.first { it.position == 1 }.title).isEqualTo("Replacement 1")
    // Untouched positions (4-10) are still present; total is unchanged, not doubled.
    assertThat(dao.count()).isEqualTo(10)
    assertThat(rows.map { it.position }.toSet()).hasSize(10)
  }

  @Test
  fun getRemotePreservesIdsForSparseNonContiguousPositions() {
    runTest(testDispatcher) {
      // Server may number positions sparsely (here 1, 5, 10). The IN-list lookup must still match
      // each remote item to its cached row by (path, position) and preserve ids — a position-range
      // (BETWEEN) approach would be the trap this avoids on memory, but correctness must hold too.
      val remoteData =
        listOf(0, 4, 9).map { index ->
          val entity = fakeAlbumQueue[index]
          NowPlayingDto(
            title = entity.title,
            artist = entity.artist,
            path = entity.path,
            position = entity.position
          )
        }
      every { playbackApi.getNowPlayingList(any()) } returns flowOf(remoteData)

      repository.getRemote(null)

      val resynced = dao.all().sortedBy { it.position }
      assertThat(resynced.map { it.position }).containsExactly(1, 5, 10).inOrder()
      assertThat(resynced.map { it.id }).containsExactly(1L, 5L, 10L).inOrder()
    }
  }

  @Test
  fun syncProgressReflectsRefreshAndClearsOnCompletion() {
    runTest(testDispatcher) {
      // Park the refresh after its first page so we can observe a non-null progress value, the way
      // the screen would while a sync is in flight. The flow drives the progress callback exactly
      // as ApiBase.getAllPages does on the real path.
      val blocker = CompletableDeferred<Unit>()
      val page = listOf(NowPlayingDto("T1", "A", "/remote/t1.mp3", 1))
      every { playbackApi.getNowPlayingList(any()) } answers {
        val callback = firstArg<Progress?>()
        flow {
          callback?.invoke(1, 1)
          emit(page)
          blocker.await()
        }
      }
      dao.deleteAll()

      assertThat(repository.syncProgress().value).isNull()

      val inflight = launch { repository.getRemote(null) }
      advanceUntilIdle()

      val progress = repository.syncProgress().value
      assertThat(progress).isNotNull()
      assertThat(progress!!.current).isEqualTo(1)
      assertThat(progress.total).isEqualTo(1)
      assertThat(progress.isDeterminate).isTrue()

      blocker.complete(Unit)
      advanceUntilIdle()

      assertThat(repository.syncProgress().value).isNull()
      inflight.join()
    }
  }

  @Test
  fun getAllReturnsPagingDataFlowFromDao() {
    runTest(testDispatcher) {
      val pagingData = repository.getAll()

      val currentItems = pagingData.asSnapshot()
      assertThat(currentItems).hasSize(10)
      assertThat(
        currentItems.map {
          it.path
        }
      ).containsExactlyElementsIn(fakeAlbumQueue.map { it.path })
    }
  }

  @Test
  fun observeIndexOfPathReturnsTheZeroBasedListIndexOfTheTrack() {
    runTest(testDispatcher) {
      // Position 8 in the fixture, so the eighth row of an `order by position` list.
      val track = fakeAlbumQueue.first { it.position == 8 }

      val index = repository.observeIndexOfPath(track.path).first()

      assertThat(index).isEqualTo(7)
    }
  }

  @Test
  fun observeIndexOfPathCountsRowsSoGapsInPositionDoNotShiftTheIndex() {
    runTest(testDispatcher) {
      // A partially applied reorder (or a server numbering positions from something other than 1)
      // leaves gaps. The index must follow the row order, not the raw position value.
      dao.deleteAll()
      dao.insertAll(
        listOf(
          NowPlayingEntity(title = "A", path = "/a.mp3", position = 40, id = 1),
          NowPlayingEntity(title = "B", path = "/b.mp3", position = 90, id = 2),
          NowPlayingEntity(title = "C", path = "/c.mp3", position = 91, id = 3)
        )
      )

      assertThat(repository.observeIndexOfPath("/a.mp3").first()).isEqualTo(0)
      assertThat(repository.observeIndexOfPath("/b.mp3").first()).isEqualTo(1)
      assertThat(repository.observeIndexOfPath("/c.mp3").first()).isEqualTo(2)
    }
  }

  @Test
  fun observeIndexOfPathResolvesADuplicatedTrackToItsFirstOccurrence() {
    runTest(testDispatcher) {
      dao.deleteAll()
      dao.insertAll(
        listOf(
          NowPlayingEntity(title = "A", path = "/a.mp3", position = 1, id = 1),
          NowPlayingEntity(title = "B", path = "/dup.mp3", position = 2, id = 2),
          NowPlayingEntity(title = "C", path = "/c.mp3", position = 3, id = 3),
          NowPlayingEntity(title = "B", path = "/dup.mp3", position = 4, id = 4)
        )
      )

      assertThat(repository.observeIndexOfPath("/dup.mp3").first()).isEqualTo(1)
    }
  }

  @Test
  fun observeIndexOfPathReturnsNullWhenTheQueueDoesNotHoldTheTrack() {
    runTest(testDispatcher) {
      val index = repository.observeIndexOfPath("/music/not/queued.mp3").first()

      assertThat(index).isNull()
    }
  }

  @Test
  fun observeIndexOfPathReturnsNullForABlankPathWithoutQueryingTheQueue() {
    runTest(testDispatcher) {
      // Nothing has played yet, so the playing track carries an empty path.
      assertThat(repository.observeIndexOfPath("").first()).isNull()
    }
  }

  @Test
  fun observeIndexOfPathEmitsOnceTheSyncReachesTheTrack() {
    runTest(testDispatcher) {
      // The screen asks for the index before the queue has synced that far; the lookup has to
      // resolve on its own when the page holding the track lands.
      dao.deleteAll()

      repository.observeIndexOfPath("/late.mp3").test {
        assertThat(awaitItem()).isNull()

        dao.insertAll(
          listOf(
            NowPlayingEntity(title = "A", path = "/a.mp3", position = 1, id = 1),
            NowPlayingEntity(title = "Late", path = "/late.mp3", position = 2, id = 2)
          )
        )

        assertThat(awaitItem()).isEqualTo(1)
        cancelAndIgnoreRemainingEvents()
      }
    }
  }

  @Test
  fun searchReturnsPagingDataFlowForMatchingTerm() {
    runTest(testDispatcher) {
      val searchTerm = "Lost in the City"

      val pagingData = repository.search(searchTerm)

      val actual = pagingData.asSnapshot()
      assertThat(actual.map { it.path }).containsExactly(fakeAlbumQueue[3].path)
    }
  }
}
