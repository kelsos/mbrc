package com.kelsos.mbrc.features.nowplaying

import androidx.paging.testing.asSnapshot
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import com.kelsos.mbrc.data.Database
import com.kelsos.mbrc.networking.ApiBase
import com.kelsos.mbrc.utils.testDispatcher
import com.kelsos.mbrc.utils.testDispatcherModule
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
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
  private val testModule =
    module {
      single<ApiBase> { mockk(relaxed = true) }
      single {
        Room
          .inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            Database::class.java,
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
  private val api: ApiBase by inject()

  private val repository: NowPlayingRepository by inject()

  val fakeAlbumQueue =
    listOf(
      NowPlayingEntity(
        title = "Midnight Dreams",
        artist = "The Electric Horizon",
        path = "/music/electric_horizon/midnight_dreams/01_midnight_dreams.mp3",
        position = 1,
        dateAdded = System.currentTimeMillis() - 3600000, // 1 hour ago
        id = 1,
      ),
      NowPlayingEntity(
        title = "Neon Lights",
        artist = "The Electric Horizon",
        path = "/music/electric_horizon/midnight_dreams/02_neon_lights.mp3",
        position = 2,
        dateAdded = System.currentTimeMillis() - 3590000,
        id = 2,
      ),
      NowPlayingEntity(
        title = "Digital Rain",
        artist = "The Electric Horizon",
        path = "/music/electric_horizon/midnight_dreams/03_digital_rain.mp3",
        position = 3,
        dateAdded = System.currentTimeMillis() - 3580000,
        id = 3,
      ),
      NowPlayingEntity(
        title = "Lost in the City",
        artist = "The Electric Horizon",
        path = "/music/electric_horizon/midnight_dreams/04_lost_in_the_city.mp3",
        position = 4,
        dateAdded = System.currentTimeMillis() - 3570000,
        id = 4,
      ),
      NowPlayingEntity(
        title = "Synth Wave",
        artist = "The Electric Horizon",
        path = "/music/electric_horizon/midnight_dreams/05_synth_wave.mp3",
        position = 5,
        dateAdded = System.currentTimeMillis() - 3560000,
        id = 5,
      ),
      NowPlayingEntity(
        title = "Circuit Breaker",
        artist = "The Electric Horizon",
        path = "/music/electric_horizon/midnight_dreams/06_circuit_breaker.mp3",
        position = 6,
        dateAdded = System.currentTimeMillis() - 3550000,
        id = 6,
      ),
      NowPlayingEntity(
        title = "Electric Pulse",
        artist = "The Electric Horizon",
        path = "/music/electric_horizon/midnight_dreams/07_electric_pulse.mp3",
        position = 7,
        dateAdded = System.currentTimeMillis() - 3540000,
        id = 7,
      ),
      NowPlayingEntity(
        title = "Data Stream",
        artist = "The Electric Horizon",
        path = "/music/electric_horizon/midnight_dreams/08_data_stream.mp3",
        position = 8,
        dateAdded = System.currentTimeMillis() - 3530000,
        id = 8,
      ),
      NowPlayingEntity(
        title = "Virtual Reality",
        artist = "The Electric Horizon",
        path = "/music/electric_horizon/midnight_dreams/09_virtual_reality.mp3",
        position = 9,
        dateAdded = System.currentTimeMillis() - 3520000,
        id = 9,
      ),
      NowPlayingEntity(
        title = "Dawn Protocol",
        artist = "The Electric Horizon",
        path = "/music/electric_horizon/midnight_dreams/10_dawn_protocol.mp3",
        position = 10,
        dateAdded = System.currentTimeMillis() - 3510000,
        id = 10,
      ),
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
      assertThat(postMoveAlbumQueue.map { it.position }).containsExactly(1, 2, 3, 4, 5, 6, 7, 8, 9, 10).inOrder()

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
      assertThat(postMoveAlbumQueue.map { it.id }).containsExactlyElementsIn(expectedIdOrder).inOrder()
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
      assertThat(postMoveAlbumQueue.map { it.position }).containsExactly(1, 2, 3, 4, 5, 6, 7, 8, 9, 10).inOrder()

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
      assertThat(postMoveAlbumQueue.map { it.id }).containsExactlyElementsIn(expectedIdOrder).inOrder()
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
      val itemsAfterPosition = dao.all().filter { it.position != null && it.position > position }

      repository.remove(position)

      val afterRemoval = dao.all().sortedBy { it.position }

      // Verify count decreased by 1
      assertThat(dao.count()).isEqualTo(originalCount - 1)

      // Verify the specific item was removed (not just that position 4 is empty)
      assertThat(afterRemoval.none { it.id == itemToRemove?.id }).isTrue()

      // Verify positions are consecutive 1 through (originalCount-1)
      assertThat(afterRemoval.map { it.position }).containsExactly(1, 2, 3, 4, 5, 6, 7, 8, 9).inOrder()

      // Verify items after the removed position shifted down correctly
      itemsAfterPosition.forEach { originalItem ->
        val updatedItem = dao.all().find { it.id == originalItem.id }
        assertThat(updatedItem?.position).isEqualTo(originalItem.position!! - 1)
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
      val itemsAfterPosition = dao.all().filter { it.position != null && it.position > position }

      repository.remove(position)

      val afterRemoval = dao.all().sortedBy { it.position }

      // Verify count decreased by 1
      assertThat(dao.count()).isEqualTo(originalCount - 1)

      // Verify the specific item was removed (not just that position 3 is empty)
      assertThat(afterRemoval.none { it.id == itemToRemove?.id }).isTrue()

      // Verify positions are consecutive 1 through (originalCount-1)
      assertThat(afterRemoval.map { it.position }).containsExactly(1, 2, 3, 4, 5, 6, 7, 8, 9).inOrder()

      // Verify items after the removed position shifted down correctly
      itemsAfterPosition.forEach { originalItem ->
        val updatedItem = dao.all().find { it.id == originalItem.id }
        assertThat(updatedItem?.position).isEqualTo(originalItem.position!! - 1)
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
            position = 1,
          ),
          NowPlayingDto(
            title = "Remote Song 2",
            artist = "Remote Artist 2",
            path = "/remote/path2.mp3",
            position = 2,
          ),
        )

      coEvery {
        api.getAllPages(any(), NowPlayingDto::class, any())
      } returns flowOf(remoteData)

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
  fun getAllReturnsPagingDataFlowFromDao() {
    runTest(testDispatcher) {
      val pagingData = repository.getAll()

      val currentItems = pagingData.asSnapshot()
      assertThat(currentItems).hasSize(10)
      assertThat(currentItems.map { it.path }).containsExactlyElementsIn(fakeAlbumQueue.map { it.path })
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
