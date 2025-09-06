package com.kelsos.mbrc.app

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.navigation.compose.rememberNavController
import com.kelsos.mbrc.R
import com.kelsos.mbrc.common.ui.compose.DrawerNavigationIcon
import com.kelsos.mbrc.common.ui.compose.RemoteTopAppBar
import com.kelsos.mbrc.features.settings.SettingsManager
import com.kelsos.mbrc.features.theme.Theme
import com.kelsos.mbrc.theme.RemoteTheme
import kotlinx.coroutines.launch
import org.koin.androidx.compose.koinViewModel
import org.koin.compose.koinInject

/**
 * Main composable container for the MusicBee Remote app.
 * Sets up the theme, navigation, drawer, and scaffold structure.
 */
@Composable
fun RemoteApp() {
  val settingsManager: SettingsManager = koinInject()
  val themeState by settingsManager.themeFlow.collectAsState(initial = Theme.System)

  val darkTheme = when (themeState) {
    Theme.Light -> false
    Theme.Dark -> true
    Theme.System -> isSystemInDarkTheme()
  }

  RemoteTheme(darkTheme = darkTheme) {
    val navController = rememberNavController()
    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val drawerViewModel: DrawerViewModel = koinViewModel()

    // Screen configuration state
    var currentScreenConfig by remember { mutableStateOf(ScreenConfig.Empty) }

    // Handle screen-level snackbar messages
    LaunchedEffect(currentScreenConfig.snackbarMessages) {
      currentScreenConfig.snackbarMessages?.collect { message ->
        snackbarHostState.showSnackbar(message)
      }
    }

    ModalNavigationDrawer(
      drawerState = drawerState,
      drawerContent = {
        AppDrawer(
          drawerState = drawerState,
          navController = navController,
          drawerViewModel = drawerViewModel
        )
      }
    ) {
      Scaffold(
        topBar = {
          if (!currentScreenConfig.hasCustomTopBar) {
            RemoteTopAppBar(
              title = getCurrentScreenTitle(navController),
              navigationIcon = {
                DrawerNavigationIcon(
                  onClick = {
                    scope.launch {
                      drawerState.open()
                    }
                  }
                )
              },
              actions = currentScreenConfig.topBarActions ?: {}
            )
          }
        },
        floatingActionButton = {
          currentScreenConfig.floatingActionButton?.invoke()
        },
        snackbarHost = {
          SnackbarHost(hostState = snackbarHostState)
        }
      ) { paddingValues ->
        Box(
          modifier = Modifier
            .fillMaxSize()
            .padding(paddingValues)
        ) {
          AppNavGraph(
            navController = navController,
            startDestination = Screen.Home.route,
            onScreenConfigChange = { config ->
              currentScreenConfig = config
            }
          )
        }
      }
    }
  }
}

/**
 * Gets the title for the current screen based on the navigation route.
 * TODO: This should eventually be managed by each screen's ViewModel
 */
@Composable
private fun getCurrentScreenTitle(navController: androidx.navigation.NavController): String {
  val currentRoute = navController.currentBackStackEntry?.destination?.route

  return currentRoute?.let { route ->
    getScreenTitleForRoute(route)
  } ?: stringResource(R.string.application_name)
}

@Composable
private fun getScreenTitleForRoute(route: String): String = when (route) {
  Screen.Home.route -> stringResource(R.string.nav_now_playing)
  Screen.Library.route -> stringResource(R.string.nav_library)
  Screen.Playlists.route -> stringResource(R.string.nav_playlists)
  Screen.Radio.route -> stringResource(R.string.nav_radio)
  Screen.Settings.route -> stringResource(R.string.nav_settings)
  Screen.Help.route -> stringResource(R.string.nav_help)
  Screen.ConnectionManager.route -> stringResource(R.string.connection_manager_title)
  Screen.NowPlayingList.route -> stringResource(R.string.menu_queue_next)
  Screen.Lyrics.route -> stringResource(R.string.nav_lyrics)
  else -> getDetailScreenTitle(route)
}

@Composable
private fun getDetailScreenTitle(route: String): String = when {
  route.startsWith("album_tracks") -> ""
  route.startsWith("artist_albums") -> ""
  route.startsWith("genre_artists") -> ""
  route.startsWith("playlist_tracks") -> ""
  else -> stringResource(R.string.application_name)
}
