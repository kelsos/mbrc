package com.kelsos.mbrc.features.nowplaying.presentation

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.paging.PagedList
import com.kelsos.mbrc.content.activestatus.livedata.PlayingTrackState
import com.kelsos.mbrc.features.nowplaying.domain.NowPlaying
import com.kelsos.mbrc.features.nowplaying.repository.NowPlayingRepository
import com.kelsos.mbrc.events.Event
import com.kelsos.mbrc.events.UserAction
import com.kelsos.mbrc.features.nowplaying.domain.MoveManager
import com.kelsos.mbrc.networking.client.UserActionUseCase
import com.kelsos.mbrc.networking.protocol.NowPlayingMoveRequest
import com.kelsos.mbrc.networking.protocol.Protocol
import com.kelsos.mbrc.utilities.AppCoroutineDispatchers
import com.kelsos.mbrc.utilities.paged
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class NowPlayingViewModel(
  val playingTrack: PlayingTrackState,
  private val repository: NowPlayingRepository,
  private val moveManager: MoveManager,
  private val dispatchers: AppCoroutineDispatchers,
  private val userActionUseCase: UserActionUseCase
) : ViewModel() {

  private val viewModelJob: Job = Job()
  private val networkScope: CoroutineScope = CoroutineScope(dispatchers.network + viewModelJob)
  private val eventStream: MutableLiveData<Event<Int>> = MutableLiveData()

  val nowPlayingTracks: LiveData<PagedList<NowPlaying>> = repository.getAll().paged()
  val events: LiveData<Event<Int>>
    get() = eventStream

  init {
    moveManager.onMoveSubmit { originalPosition, finalPosition ->
      val data = NowPlayingMoveRequest(originalPosition, finalPosition)
      userActionUseCase.perform(UserAction(Protocol.NowPlayingListMove, data))
    }
  }

  fun refresh() {
    networkScope.launch {
      try {
        repository.getRemote()
      } catch (ex: Exception) {
        withContext(dispatchers.main) {
          eventStream.value = Event(0)
        }
      }
    }
  }

  fun search(query: String) {
    // todo: drop and upgrade to do this locally,
  }

  fun moveTrack(from: Int, to: Int) {
    networkScope.launch {
      moveManager.move(from, to)
    }
  }

  fun play(position: Int) {
    userActionUseCase.perform(UserAction(Protocol.NowPlayingListPlay, position))
  }

  fun removeTrack(position: Int) {
    userActionUseCase.perform(UserAction(Protocol.NowPlayingListRemove, position))
  }

  override fun onCleared() {
    viewModelJob.cancel()
    super.onCleared()
  }
}