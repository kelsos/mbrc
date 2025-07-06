package com.kelsos.mbrc.common.utilities

import kotlin.coroutines.CoroutineContext
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob

open class ScopeBase(private val dispatcher: CoroutineDispatcher) : CoroutineScope {
  private var supervisorJob: Job? = null
  private val job: Job
    get() = supervisorJob ?: SupervisorJob().also { supervisorJob = it }

  override val coroutineContext: CoroutineContext
    get() = job + dispatcher

  fun onStart() {
    if (supervisorJob?.isCancelled == true) {
      supervisorJob = SupervisorJob()
    }
  }

  fun onStop() {
    job.cancel()
  }
}
