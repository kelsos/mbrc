package com.kelsos.mbrc.data

import com.kelsos.mbrc.common.utilities.AppCoroutineDispatchers
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class MigrationManager(
  private val defaultConnectionMigration: DefaultConnectionMigration,
  dispatchers: AppCoroutineDispatchers
) {
  private val migrationScope = CoroutineScope(SupervisorJob() + dispatchers.database)

  fun runMigrations() {
    runDefaultConnectionMigration()
  }

  private fun runDefaultConnectionMigration() {
    migrationScope.launch {
      defaultConnectionMigration.migrate()
    }
  }
}
