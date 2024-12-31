package com.kelsos.mbrc.di.modules

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import toothpick.Toothpick
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ViewModelFactory
  @Inject
  constructor(
    private val app: Application,
  ) : ViewModelProvider.NewInstanceFactory() {
    override fun <T : ViewModel> create(modelClass: Class<T>) = Toothpick.openScope(app).getInstance(modelClass) as T
  }
