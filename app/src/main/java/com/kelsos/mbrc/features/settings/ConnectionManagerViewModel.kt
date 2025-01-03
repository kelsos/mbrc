package com.kelsos.mbrc.features.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import androidx.paging.cachedIn
import com.kelsos.mbrc.common.utilities.AppCoroutineDispatchers
import com.kelsos.mbrc.networking.discovery.DiscoveryStop
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

interface IConnectionManagerActions {
  val startDiscovery: () -> Unit
  val setDefault: (settings: ConnectionSettings) -> Unit
  val save: (settings: ConnectionSettings) -> Unit
  val delete: (settings: ConnectionSettings) -> Unit
}

class ConnectionManagerActions(
  repository: ConnectionRepository,
  viewModelScope: CoroutineScope,
  dispatchers: AppCoroutineDispatchers,
  events: MutableSharedFlow<DiscoveryStop>,
) : IConnectionManagerActions {
  override val startDiscovery: () -> Unit = {
    viewModelScope.launch(dispatchers.network) {
      val result = repository.discover()
      withContext(dispatchers.main) {
        events.emit(result)
      }
    }
  }
  override val setDefault: (settings: ConnectionSettings) -> Unit = {
    viewModelScope.launch(dispatchers.database) {
      repository.setDefault(it)
    }
  }

  override val save: (settings: ConnectionSettings) -> Unit = {
    viewModelScope.launch(dispatchers.database) {
      repository.save(it)
    }
  }

  override val delete: (settings: ConnectionSettings) -> Unit = {
    viewModelScope.launch(dispatchers.database) {
      repository.delete(it)
    }
  }
}

data class ConnectionManagerState(
  val events: Flow<DiscoveryStop>,
  val settings: Flow<PagingData<ConnectionSettings>>,
)

class ConnectionManagerViewModel(
  repository: ConnectionRepository,
  dispatchers: AppCoroutineDispatchers,
) : ViewModel() {
  private val events: MutableSharedFlow<DiscoveryStop> = MutableSharedFlow()
  val actions: IConnectionManagerActions =
    ConnectionManagerActions(
      repository,
      viewModelScope,
      dispatchers,
      events,
    )
  val state =
    ConnectionManagerState(
      events = events,
      settings = repository.getAll().cachedIn(viewModelScope),
    )
}
