package com.kelsos.mbrc.common.utilities

import kotlinx.coroutines.CoroutineDispatcher

data class AppCoroutineDispatchers(
  val main: CoroutineDispatcher,
  val io: CoroutineDispatcher,
  val database: CoroutineDispatcher,
  val network: CoroutineDispatcher,
)
