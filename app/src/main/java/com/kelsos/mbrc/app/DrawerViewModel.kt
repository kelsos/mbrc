package com.kelsos.mbrc.app

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kelsos.mbrc.common.state.ConnectionStateFlow
import com.kelsos.mbrc.common.state.ConnectionStatus
import com.kelsos.mbrc.networking.ClientConnectionUseCase
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class DrawerViewModel(
  private val connectionStateFlow: ConnectionStateFlow,
  private val clientConnectionUseCase: ClientConnectionUseCase
) : ViewModel() {

  val connectionStatus: StateFlow<ConnectionStatus> = connectionStateFlow.connection

  fun isConnected(): Boolean = connectionStatus.value is ConnectionStatus.Connected

  fun toggleConnection() {
    viewModelScope.launch {
      if (isConnected()) {
        clientConnectionUseCase.disconnect()
      } else {
        clientConnectionUseCase.connect()
      }
    }
  }
}
