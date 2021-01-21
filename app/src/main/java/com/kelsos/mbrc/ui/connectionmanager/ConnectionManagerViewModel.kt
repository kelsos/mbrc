package com.kelsos.mbrc.ui.connectionmanager

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.kelsos.mbrc.common.utilities.AppDispatchers
import com.kelsos.mbrc.events.Event
import com.kelsos.mbrc.networking.connections.ConnectionRepository
import com.kelsos.mbrc.networking.connections.ConnectionSettingsEntity
import com.kelsos.mbrc.networking.discovery.DiscoveryStop
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ConnectionManagerViewModel(
  private val repository: ConnectionRepository,
  private val dispatchers: AppDispatchers
) : ViewModel() {

  private val job: Job = Job()
  private val scope = CoroutineScope(dispatchers.database + job)
  private val _discoveryStatus: MutableLiveData<Event<DiscoveryStop>> = MutableLiveData()
  val settings: LiveData<List<ConnectionSettingsEntity>> = repository.getAll()
  val discoveryStatus: LiveData<Event<DiscoveryStop>> get() = _discoveryStatus
  val defaultConnectionId = repository.getDefaultConnectionId()

  fun startDiscovery() {
    scope.launch {
      val result = repository.discover()
      withContext(dispatchers.main) {
        _discoveryStatus.value = Event(result)
      }
    }
  }

  fun setDefault(settings: ConnectionSettingsEntity) {
    scope.launch {
      repository.setDefaultConnectionId(settings.id)
    }
  }

  fun save(settings: ConnectionSettingsEntity) {
    scope.launch {
      repository.save(settings)
    }
  }

  fun delete(settings: ConnectionSettingsEntity) {
    scope.launch {
      repository.delete(settings)
    }
  }

  override fun onCleared() {
    job.cancel()
    super.onCleared()
  }
}
