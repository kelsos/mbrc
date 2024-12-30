package com.kelsos.mbrc.ui.connection_manager

import com.kelsos.mbrc.data.ConnectionSettings

data class ConnectionModel(val defaultId: Long, val settings: List<ConnectionSettings>)
