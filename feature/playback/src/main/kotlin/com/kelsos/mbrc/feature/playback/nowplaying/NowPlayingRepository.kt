package com.kelsos.mbrc.feature.playback.nowplaying

import androidx.paging.PagingData
import com.kelsos.mbrc.core.common.data.Progress
import com.kelsos.mbrc.core.common.utilities.coroutines.AppCoroutineDispatchers
import com.kelsos.mbrc.core.common.utilities.epoch
import com.kelsos.mbrc.core.data.Repository
import com.kelsos.mbrc.core.data.nowplaying.NowPlaying
import com.kelsos.mbrc.core.data.nowplaying.NowPlayingDao
import com.kelsos.mbrc.core.data.nowplaying.SearchResult
import com.kelsos.mbrc.core.data.paged
import com.kelsos.mbrc.core.networking.api.PlaybackApi
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

interface NowPlayingRepository : Repository<NowPlaying> {
  suspend fun move(from: Int, to: Int)

  suspend fun remove(position: Int)

  suspend fun findPosition(query: String): Int

  suspend fun searchTrack(query: String): SearchResult?

  fun observeCount(): Flow<Int>

  /**
   * Zero-based index of the first queue entry with [path], or `null` while the queue holds no such
   * entry. Emits again as the queue changes, so a lookup made before the sync reached the track
   * resolves once its page lands.
   */
  fun observeIndexOfPath(path: String): Flow<Int?>

  /**
   * Progress of the single in-flight refresh, regardless of which trigger started it
   * (screen reload, pull-to-refresh, or a `nowplayinglistchanged` broadcast). Emits `null`
   * while idle.
   */
  fun syncProgress(): StateFlow<SyncProgress?>
}

class NowPlayingRepositoryImpl(
  private val playbackApi: PlaybackApi,
  private val dao: NowPlayingDao,
  private val dispatchers: AppCoroutineDispatchers
) : NowPlayingRepository {
  // App-lifetime scope owning the single in-flight refresh. SupervisorJob keeps one refresh's
  // failure from cancelling the scope or a later refresh.
  private val refreshScope = CoroutineScope(dispatchers.network + SupervisorJob())
  private val refreshMutex = Mutex()
  private var refreshJob: Deferred<Unit>? = null

  private val syncProgressFlow = MutableStateFlow<SyncProgress?>(null)

  override fun syncProgress(): StateFlow<SyncProgress?> = syncProgressFlow.asStateFlow()

  override suspend fun count(): Long = withContext(dispatchers.database) { dao.count() }

  override fun getAll(): Flow<PagingData<NowPlaying>> = paged(
    pagingSourceFactory = { dao.getAll() },
    key = { it.id },
    enablePlaceholders = true
  ) { it.toNowPlaying() }

  /**
   * Refreshes the now-playing queue, keeping only a single refresh in flight. A newer trigger
   * (user pull-to-refresh or a `nowplayinglistchanged` broadcast) cancels any refresh already
   * running and restarts from the first page, so two refreshes never interleave their writes —
   * which previously let one refresh's `removePreviousEntries` delete the other's rows and leave
   * a truncated queue. The caller still suspends until its own refresh finishes (or fails).
   */
  override suspend fun getRemote(progress: Progress?) {
    val job =
      refreshMutex.withLock {
        refreshJob?.cancel()
        refreshScope.async { fetchAndReconcile(progress) }.also { refreshJob = it }
      }
    job.await()
  }

  private suspend fun fetchAndReconcile(progress: Progress?) {
    val added = epoch()
    // Indeterminate until the first page reports a server total.
    syncProgressFlow.value = SyncProgress(current = 0, total = 0)
    playbackApi
      .getNowPlayingList { current, total ->
        syncProgressFlow.value = SyncProgress(current, total)
        progress?.invoke(current, total)
      }
      .onCompletion { cause ->
        // Only prune stale rows after a clean run. A cancelled (superseded) or failed refresh
        // must leave the existing queue intact — the replacing/next successful refresh prunes.
        if (cause == null) {
          withContext(dispatchers.database) {
            dao.removePreviousEntries(added)
          }
        }
        // Hide the bar when this refresh ends — but not when it was superseded: the cancelling
        // refresh has already set its own progress and now owns the indicator.
        if (cause !is CancellationException) {
          syncProgressFlow.value = null
        }
      }.collect { item ->
        val entities = item.map { it.toEntity().copy(dateAdded = added) }
        withContext(dispatchers.database) {
          dao.reconcilePage(entities)
        }
      }
  }

  override fun search(term: String): Flow<PagingData<NowPlaying>> = paged({
    dao.search(term)
  }, key = { it.id }) { it.toNowPlaying() }

  override suspend fun move(from: Int, to: Int) {
    if (from == to) {
      return
    }
    withContext(dispatchers.database) {
      dao.move(from, to)
    }
  }

  override suspend fun remove(position: Int) {
    withContext(dispatchers.database) {
      dao.remove(position)
    }
  }

  override suspend fun findPosition(query: String): Int = withContext(dispatchers.database) {
    if (query.isBlank()) {
      return@withContext -1
    }
    return@withContext dao.findPositionByQuery(query) ?: -1
  }

  override suspend fun searchTrack(query: String): SearchResult? =
    withContext(dispatchers.database) {
      if (query.isBlank()) {
        return@withContext null
      }
      return@withContext dao.searchTrack(query)
    }

  override suspend fun getById(id: Long): NowPlaying? {
    return withContext(dispatchers.database) {
      val entity = dao.getById(id) ?: return@withContext null
      return@withContext entity.toNowPlaying()
    }
  }

  override fun observeCount(): Flow<Int> = dao.observeCount()

  override fun observeIndexOfPath(path: String): Flow<Int?> = if (path.isBlank()) {
    flowOf(null)
  } else {
    // A sync reconciles page by page, so the underlying query re-runs on every page write.
    // Only forward the emissions where the index actually moved.
    dao.observeIndexOfPath(path).distinctUntilChanged()
  }
}
