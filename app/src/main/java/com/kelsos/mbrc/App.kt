package com.kelsos.mbrc

import android.app.ActivityManager
import android.app.Application
import android.app.NotificationManager
import android.content.SharedPreferences
import android.media.AudioManager
import android.net.ConnectivityManager
import android.net.wifi.WifiManager
import androidx.annotation.CallSuper
import androidx.core.content.getSystemService
import androidx.preference.PreferenceManager
import com.raizlabs.android.dbflow.config.FlowConfig
import com.raizlabs.android.dbflow.config.FlowManager
import org.koin.android.ext.koin.androidContext
import org.koin.androidx.fragment.koin.fragmentFactory
import org.koin.core.context.GlobalContext.startKoin
import org.koin.core.module.Module
import org.koin.dsl.module
import timber.log.Timber

open class App : Application() {
  @CallSuper
  override fun onCreate() {
    super.onCreate()
    initialize()
  }

  protected open fun appModules(): List<Module> {
    val androidModule =
      module {
        val app = this@App as Application
        single { app.resources }
        single { checkNotNull(app.getSystemService<ActivityManager>()) }
        single { checkNotNull(app.getSystemService<AudioManager>()) }
        single { checkNotNull(app.getSystemService<NotificationManager>()) }
        single { checkNotNull(app.getSystemService<WifiManager>()) }
        single { checkNotNull(app.getSystemService<ConnectivityManager>()) }
        single<SharedPreferences> { PreferenceManager.getDefaultSharedPreferences(get()) }
      }
    return listOf(
      appModule,
      androidModule,
    )
  }

  protected open fun initialize() {
    initializeDbflow()
    startKoin {
      androidContext(this@App)
      fragmentFactory()
      modules(appModules())
    }
    initializeTimber()
  }

  private fun initializeDbflow() {
    FlowManager.init(FlowConfig.Builder(this).openDatabasesOnInit(true).build())
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
}
