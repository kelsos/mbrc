package com.kelsos.mbrc.utils

import com.kelsos.mbrc.common.utilities.AppCoroutineDispatchers
import com.squareup.moshi.Moshi
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.test.StandardTestDispatcher
import org.koin.dsl.module

val testDispatcher = StandardTestDispatcher()

val testDispatchers =
  object : AppCoroutineDispatchers {
    override val main: CoroutineDispatcher = testDispatcher
    override val io: CoroutineDispatcher = testDispatcher
    override val database: CoroutineDispatcher = testDispatcher
    override val network: CoroutineDispatcher = testDispatcher
  }

val testDispatcherModule =
  module {
    single<AppCoroutineDispatchers> {
      testDispatchers
    }
  }

val parserModule =
  module {
    single {
      Moshi.Builder().build()
    }
  }
