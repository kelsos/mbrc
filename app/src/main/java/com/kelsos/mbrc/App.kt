package com.kelsos.mbrc

import android.app.Application
import androidx.annotation.CallSuper
import com.raizlabs.android.dbflow.config.FlowConfig
import com.raizlabs.android.dbflow.config.FlowManager
import timber.log.Timber
import toothpick.Toothpick
import toothpick.configuration.Configuration
import toothpick.registries.FactoryRegistryLocator
import toothpick.registries.MemberInjectorRegistryLocator
import toothpick.smoothie.module.SmoothieApplicationModule

open class App : Application() {
  @CallSuper
  override fun onCreate() {
    super.onCreate()
    initialize()
  }

  protected open fun initialize() {
    initializeDbflow()
    initializeToothpick()
    initializeTimber()
  }

  private fun initializeTimber() {
    if (BuildConfig.DEBUG) {
      Timber.plant(
        object : Timber.DebugTree() {
          override fun createStackElementTag(element: StackTraceElement): String =
            "${super.createStackElementTag(element)}:${element.lineNumber} [${Thread.currentThread().name}]"
        },
      )
    }
  }

  private fun initializeDbflow() {
    FlowManager.init(FlowConfig.Builder(this).openDatabasesOnInit(true).build())
  }

  private fun initializeToothpick() {
    val configuration: Configuration =
      if (BuildConfig.DEBUG) {
        Configuration.forDevelopment().disableReflection()
      } else {
        Configuration.forProduction().disableReflection()
      }

    Toothpick.setConfiguration(configuration)

    MemberInjectorRegistryLocator.setRootRegistry(MemberInjectorRegistry())
    FactoryRegistryLocator.setRootRegistry(FactoryRegistry())
    val applicationScope = Toothpick.openScope(this)
    if (!testMode()) {
      applicationScope.installModules(SmoothieApplicationModule(this), AppModule())
    }
  }

  internal open fun testMode(): Boolean = false
}
