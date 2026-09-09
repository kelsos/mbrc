package com.kelsos.mbrc

import com.kelsos.mbrc.adapters.AppInfoImpl
import com.kelsos.mbrc.adapters.ChangelogResourceProviderImpl
import com.kelsos.mbrc.adapters.ClientIdProviderAdapter
import com.kelsos.mbrc.adapters.ConnectionProviderAdapter
import com.kelsos.mbrc.adapters.CoverHandlerImpl
import com.kelsos.mbrc.adapters.DefaultConnectionProviderAdapter
import com.kelsos.mbrc.adapters.GithubReleaseParserImpl
import com.kelsos.mbrc.adapters.LibrarySyncTriggerAdapter
import com.kelsos.mbrc.adapters.NowPlayingHandlerImpl
import com.kelsos.mbrc.adapters.PlayerStateHandlerImpl
import com.kelsos.mbrc.adapters.PluginVersionHandlerImpl
import com.kelsos.mbrc.adapters.ProtocolActionFactoryAdapter
import com.kelsos.mbrc.adapters.ServiceRestarterImpl
import com.kelsos.mbrc.adapters.TrackChangeNotifierImpl
import com.kelsos.mbrc.adapters.WidgetStateObserver
import com.kelsos.mbrc.core.common.state.AppState
import com.kelsos.mbrc.core.common.state.AppStateFlow
import com.kelsos.mbrc.core.common.state.AppStatePublisher
import com.kelsos.mbrc.core.common.state.ConnectionState
import com.kelsos.mbrc.core.common.state.ConnectionStateFlow
import com.kelsos.mbrc.core.common.state.ConnectionStatePublisher
import com.kelsos.mbrc.core.common.utilities.AppInfo
import com.kelsos.mbrc.core.common.utilities.coroutines.AppCoroutineDispatchers
import com.kelsos.mbrc.core.common.utilities.logging.LogHelper
import com.kelsos.mbrc.core.common.utilities.logging.LogHelperImpl
import com.kelsos.mbrc.core.data.dataModule
import com.kelsos.mbrc.core.networking.ClientIdProvider
import com.kelsos.mbrc.core.networking.ConnectionProvider
import com.kelsos.mbrc.core.networking.DefaultConnectionProvider
import com.kelsos.mbrc.core.networking.LibrarySyncTrigger
import com.kelsos.mbrc.core.networking.ProtocolActionFactory
import com.kelsos.mbrc.core.networking.networkingModule
import com.kelsos.mbrc.core.networking.protocol.actions.CoverHandler
import com.kelsos.mbrc.core.networking.protocol.actions.NowPlayingHandler
import com.kelsos.mbrc.core.networking.protocol.actions.PlayerStateHandler
import com.kelsos.mbrc.core.networking.protocol.actions.PluginVersionHandler
import com.kelsos.mbrc.core.networking.protocol.actions.TrackChangeNotifier
import com.kelsos.mbrc.core.networking.protocol.models.PageAdapterFactory
import com.kelsos.mbrc.core.platform.service.ServiceRestarter
import com.kelsos.mbrc.feature.content.contentModule
import com.kelsos.mbrc.feature.library.libraryModule
import com.kelsos.mbrc.feature.minicontrol.miniControlModule
import com.kelsos.mbrc.feature.misc.miscModule
import com.kelsos.mbrc.feature.misc.whatsnew.ChangelogResourceProvider
import com.kelsos.mbrc.feature.playback.playbackModule
import com.kelsos.mbrc.feature.settings.domain.GithubReleaseParser
import com.kelsos.mbrc.feature.settings.settingsModule
import com.kelsos.mbrc.feature.widgets.widgetsModule
import com.kelsos.mbrc.state.AppStateManager
import com.kelsos.mbrc.state.PlayingTrackCache
import com.kelsos.mbrc.state.PlayingTrackCacheImpl
import com.kelsos.mbrc.ui.DrawerViewModel
import com.squareup.moshi.Moshi
import java.util.concurrent.Executors
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExecutorCoroutineDispatcher
import kotlinx.coroutines.asCoroutineDispatcher
import org.koin.core.module.dsl.bind
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.module

/**
 * Main application Koin module.
 *
 * This module serves as the composition root, including all feature and core modules,
 * and providing app-level dependencies that require Android context or app-specific resources.
 *
 * Module structure:
 * - Core modules: networkingModule, dataModule
 * - Feature modules: libraryModule, playbackModule, contentModule, settingsModule,
 *                    miniControlModule, miscModule, widgetsModule
 *
 * This module provides:
 * - Coroutine dispatchers
 * - Adapters implementing interfaces from core modules
 * - Android system services
 * - Media session and notification components
 * - App state management
 * - App-level ViewModels
 */
val appModule = module {
  // Included Modules
  includes(
    // Core modules
    networkingModule,
    dataModule,
    // Android platform module
    androidModule,
    // Feature modules
    libraryModule,
    playbackModule,
    contentModule,
    settingsModule,
    miniControlModule,
    miscModule,
    widgetsModule
  )

  // Coroutine Dispatchers
  val networkDispatcher = createDispatcher(name = "Network", threads = 4)
  val databaseDispatcher = createDispatcher(name = "Database")

  single<AppCoroutineDispatchers> {
    @Suppress("InjectDispatcher")
    object : AppCoroutineDispatchers {
      override val main: CoroutineDispatcher = Dispatchers.Main
      override val io: CoroutineDispatcher = Dispatchers.IO
      override val database: CoroutineDispatcher = databaseDispatcher
      override val network: CoroutineDispatcher = networkDispatcher
    }
  }

  // Serialization
  single { Moshi.Builder().add(PageAdapterFactory).build() }

  // Networking Module Adapters
  // These adapters implement interfaces defined in core/networking module
  // using app-level dependencies (settings, repositories, etc.)
  singleOf(::ClientIdProviderAdapter) { bind<ClientIdProvider>() }
  singleOf(::DefaultConnectionProviderAdapter) { bind<DefaultConnectionProvider>() }
  singleOf(::ConnectionProviderAdapter) { bind<ConnectionProvider>() }
  singleOf(::LibrarySyncTriggerAdapter) { bind<LibrarySyncTrigger>() }
  singleOf(::ProtocolActionFactoryAdapter) { bind<ProtocolActionFactory>() }

  // Protocol Action Handlers
  // These adapters handle protocol messages and update app state
  singleOf(::PlayerStateHandlerImpl) { bind<PlayerStateHandler>() }
  singleOf(::TrackChangeNotifierImpl) { bind<TrackChangeNotifier>() }
  singleOf(::WidgetStateObserver)
  singleOf(::NowPlayingHandlerImpl) { bind<NowPlayingHandler>() }
  singleOf(::PluginVersionHandlerImpl) { bind<PluginVersionHandler>() }
  singleOf(::CoverHandlerImpl) { bind<CoverHandler>() }

  // Settings Module Adapters
  // GithubReleaseParser uses Moshi codegen which requires the app module
  singleOf(::GithubReleaseParserImpl) { bind<GithubReleaseParser>() }

  // App Utilities
  singleOf(::AppInfoImpl) { bind<AppInfo>() }
  singleOf(::LogHelperImpl) { bind<LogHelper>() }

  // Service Adapters
  singleOf(::ServiceRestarterImpl) { bind<ServiceRestarter>() }

  // App State Management
  singleOf(::AppStateManager)
  singleOf(::PlayingTrackCacheImpl) { bind<PlayingTrackCache>() }
  singleOf(::AppState) {
    bind<AppStateFlow>()
    bind<AppStatePublisher>()
  }
  singleOf(::ConnectionState) {
    bind<ConnectionStateFlow>()
    bind<ConnectionStatePublisher>()
  }

  // App-Level ViewModels
  singleOf(::DrawerViewModel)

  // App Resources
  singleOf(::ChangelogResourceProviderImpl) { bind<ChangelogResourceProvider>() }
}

private fun createDispatcher(name: String, threads: Int = 1): ExecutorCoroutineDispatcher {
  var threadId = 1

  return Executors
    .newFixedThreadPool(threads) { runnable ->
      val threadName = if (threads == 1) {
        "${name}Dispatcher"
      } else {
        "${name}Dispatcher-worker-${threadId++}"
      }
      Thread(runnable, threadName)
    }.asCoroutineDispatcher()
}
