package com.kelsos.mbrc

import android.app.Application
import android.os.StrictMode
import androidx.annotation.CallSuper
import coil3.ImageLoader
import coil3.SingletonImageLoader
import coil3.request.crossfade
import com.kelsos.mbrc.adapters.WidgetStateObserver
import com.kelsos.mbrc.core.common.utilities.coroutines.AppCoroutineDispatchers
import com.kelsos.mbrc.core.common.utilities.logging.CustomLoggingTree
import com.kelsos.mbrc.core.data.migration.MigrationManager
import com.kelsos.mbrc.feature.settings.theme.ThemeManager
import com.kelsos.mbrc.feature.widgets.glance.WidgetTrampolineGuard
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import org.koin.android.ext.android.inject
import org.koin.android.ext.koin.androidContext
import org.koin.androidx.fragment.koin.fragmentFactory
import org.koin.androidx.workmanager.koin.workManagerFactory
import org.koin.core.context.GlobalContext.startKoin
import org.koin.core.module.Module
import timber.log.Timber

open class App : Application() {
  private val appScope = MainScope()
  val migrationManager: MigrationManager by inject()
  val themeManager: ThemeManager by inject()
  private val dispatchers: AppCoroutineDispatchers by inject()
  private val widgetStateObserver: WidgetStateObserver by inject()

  @CallSuper
  override fun onCreate() {
    enableStrictMode()
    super.onCreate()
    SingletonImageLoader.setSafe { context ->
      ImageLoader
        .Builder(context)
        .crossfade(true)
        .build()
    }
    initialize()
  }

  protected open fun appModules(): List<Module> = listOf(appModule)

  protected open fun initialize() {
    startKoin {
      androidContext(this@App)
      fragmentFactory()
      workManagerFactory()
      modules(appModules())
    }
    initializeTimber()
    WidgetTrampolineGuard.install(this)
    themeManager.applyTheme()
    widgetStateObserver.start(appScope)
    // Dispatched off the main thread for the injection, not just for the work. runMigrations
    // switches to the database dispatcher itself, but resolving migrationManager is what builds
    // the Room database, and StrictMode catches that read on the main thread.
    appScope.launch(dispatchers.io) {
      migrationManager.runMigrations()
    }
  }

  private fun initializeTimber() {
    if (BuildConfig.DEBUG) {
      Timber.plant(CustomLoggingTree.create())
    }
  }

  /**
   * Reports main-thread disk and network work, and the leaks the VM policy can see, on debug
   * builds.
   *
   * Logging rather than crashing: the policies catch third-party and framework work this project
   * cannot fix, so a death penalty would make debug builds unusable rather than make anyone fix
   * anything. The signal is a Timber-tagged StrictMode entry in logcat.
   *
   * Worth having because the blocking Palette call behind the player's jank was exactly this
   * shape, and nothing in the build told us about it.
   */
  private fun enableStrictMode() {
    if (!BuildConfig.DEBUG) {
      return
    }
    StrictMode.setThreadPolicy(
      StrictMode.ThreadPolicy.Builder()
        .detectDiskReads()
        .detectDiskWrites()
        .detectNetwork()
        .detectCustomSlowCalls()
        .penaltyLog()
        .build()
    )
    StrictMode.setVmPolicy(
      StrictMode.VmPolicy.Builder()
        .detectLeakedSqlLiteObjects()
        .detectLeakedClosableObjects()
        .detectLeakedRegistrationObjects()
        .detectActivityLeaks()
        .penaltyLog()
        .build()
    )
  }
}
