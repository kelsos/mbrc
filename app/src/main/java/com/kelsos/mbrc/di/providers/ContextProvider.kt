package com.kelsos.mbrc.di.providers

import android.app.Application
import android.content.Context
import javax.inject.Inject
import javax.inject.Provider

class ContextProvider
@Inject
constructor(private val application: Application) : Provider<Context> {

  override fun get(): Context {
    return application.applicationContext
  }
}
