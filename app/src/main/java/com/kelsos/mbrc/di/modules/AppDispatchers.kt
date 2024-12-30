package com.kelsos.mbrc.di.modules

import kotlinx.coroutines.CoroutineDispatcher

data class AppDispatchers(
  val main: CoroutineDispatcher,
  val io: CoroutineDispatcher,
  val db: CoroutineDispatcher,
)
