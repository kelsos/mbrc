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
import coil3.ImageLoader
import coil3.SingletonImageLoader
import coil3.request.crossfade
import com.kelsos.mbrc.common.utilities.CustomLoggingTree
import com.kelsos.mbrc.features.theme.ThemeManager
import org.koin.android.ext.android.inject
import org.koin.android.ext.koin.androidContext
import org.koin.androidx.fragment.koin.fragmentFactory
import org.koin.androidx.workmanager.koin.workManagerFactory
import org.koin.core.context.GlobalContext.startKoin
import org.koin.core.module.Module
import org.koin.dsl.module
import timber.log.Timber

open class App : Application() {
  @CallSuper
  override fun onCreate() {
    super.onCreate()
    SingletonImageLoader.setSafe { context ->
      ImageLoader
        .Builder(context)
        .crossfade(true)
        .build()
    }
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
        single<SharedPreferences> { PreferenceManager.getDefaultSharedPreferences(app) }
      }
    return listOf(
      appModule,
      androidModule,
    )
  }

  protected open fun initialize() {
    startKoin {
      androidContext(this@App)
      fragmentFactory()
      workManagerFactory()
      modules(appModules())
    }
    initializeTimber()
    initializeTheme()
  }

  private fun initializeTheme() {
    val themeManager: ThemeManager by inject()
    themeManager.applyTheme()
  }

  private fun initializeTimber() {
    if (BuildConfig.DEBUG) {
      Timber.plant(CustomLoggingTree.create())
    }
  }
}
