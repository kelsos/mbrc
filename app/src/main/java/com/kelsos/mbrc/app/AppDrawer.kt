package com.kelsos.mbrc.app

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Help
import androidx.compose.material.icons.automirrored.filled.PlaylistPlay
import androidx.compose.material.icons.automirrored.filled.QueueMusic
import androidx.compose.material.icons.filled.Circle
import androidx.compose.material.icons.filled.DesktopWindows
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.filled.Radio
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material.icons.filled.WifiOff
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DrawerState
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.NavigationDrawerItemColors
import androidx.compose.material3.NavigationDrawerItemDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.navigation.compose.currentBackStackEntryAsState
import com.kelsos.mbrc.R
import com.kelsos.mbrc.common.state.ConnectionStatus
import com.kelsos.mbrc.theme.connection_status_card_bg_dark
import com.kelsos.mbrc.theme.connection_status_card_bg_light
import com.kelsos.mbrc.theme.connection_status_connected
import com.kelsos.mbrc.theme.connection_status_offline
import com.kelsos.mbrc.theme.drawer_header_gradient_bottom_dark
import com.kelsos.mbrc.theme.drawer_header_gradient_bottom_light
import com.kelsos.mbrc.theme.drawer_header_gradient_top_dark
import com.kelsos.mbrc.theme.drawer_header_gradient_top_light
import kotlinx.coroutines.launch

/**
 * Navigation drawer for the MusicBee Remote app.
 * Displays navigation items, connection status, and app info.
 */
@Composable
fun AppDrawer(
  drawerState: DrawerState,
  navController: NavController,
  drawerViewModel: DrawerViewModel,
  modifier: Modifier = Modifier
) {
  val currentBackStackEntry by navController.currentBackStackEntryAsState()
  val currentRoute = currentBackStackEntry?.destination?.route
  val scope = rememberCoroutineScope()
  val connectionStatus by drawerViewModel.connectionStatus.collectAsState()

  ModalDrawerSheet(
    modifier = modifier,
    drawerContainerColor = MaterialTheme.colorScheme.surface
  ) {
    Column(
      modifier = Modifier
        .fillMaxSize()
        .verticalScroll(rememberScrollState())
    ) {
      // Header with app name and connection status
      DrawerHeader(
        connectionStatus = connectionStatus,
        onConnectionToggle = { drawerViewModel.toggleConnection() }
      )

      // Main navigation items
      DrawerNavigationItems(
        currentRoute = currentRoute,
        onNavigate = { screen ->
          scope.launch {
            drawerState.close()
            navController.navigate(screen.route) {
              // Pop up to the start destination to avoid building up a back stack
              popUpTo(navController.graph.startDestinationId) {
                saveState = true
              }
              // Avoid multiple copies of the same destination
              launchSingleTop = true
              // Restore state when navigating back to a destination
              restoreState = true
            }
          }
        }
      )

      Spacer(modifier = Modifier.weight(1f))

      // Footer with connection manager
      HorizontalDivider(
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
        color = MaterialTheme.colorScheme.outlineVariant
      )

      DrawerNavigationItem(
        item = DrawerItem(
          Screen.ConnectionManager,
          Icons.Default.DesktopWindows,
          R.string.connection_manager_title
        ),
        currentRoute = currentRoute,
        onNavigate = { }, // Not used since we provide custom onClick
        colors = NavigationDrawerItemDefaults.colors(
          selectedContainerColor = MaterialTheme.colorScheme.tertiaryContainer,
          selectedIconColor = MaterialTheme.colorScheme.tertiary,
          selectedTextColor = MaterialTheme.colorScheme.tertiary
        ),
        onClick = {
          scope.launch {
            drawerState.close()
            navController.navigate(Screen.ConnectionManager.route) {
              // Pop up to the start destination to avoid building up a back stack
              popUpTo(navController.graph.startDestinationId) {
                saveState = true
              }
              // Avoid multiple copies of the same destination
              launchSingleTop = true
              // Restore state when navigating back to a destination
              restoreState = true
            }
          }
        }
      )

      Spacer(modifier = Modifier.height(16.dp))
    }
  }
}

/**
 * Header section of the drawer showing app branding and connection status.
 */
@Composable
private fun DrawerHeader(connectionStatus: ConnectionStatus, onConnectionToggle: () -> Unit) {
  val isDarkTheme = isSystemInDarkTheme()

  Box(
    modifier = Modifier
      .fillMaxWidth()
      .height(180.dp)
      .background(
        Brush.verticalGradient(
          colors = if (isDarkTheme) {
            listOf(
              drawer_header_gradient_top_dark,
              drawer_header_gradient_bottom_dark
            )
          } else {
            listOf(
              drawer_header_gradient_top_light,
              drawer_header_gradient_bottom_light
            )
          }
        )
      )
  ) {
    Column(
      modifier = Modifier
        .fillMaxSize()
        .padding(24.dp),
      verticalArrangement = Arrangement.Bottom
    ) {
      // App logo and name in a row
      Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp)
      ) {
        // App icon with rounded container
        Surface(
          modifier = Modifier.size(72.dp),
          shape = RoundedCornerShape(16.dp),
          color = Color.White.copy(alpha = 0.2f)
        ) {
          Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
          ) {
            Image(
              painter = painterResource(id = R.mipmap.ic_launcher),
              contentDescription = null,
              modifier = Modifier.size(56.dp)
            )
          }
        }

        // App name next to the logo
        Text(
          text = stringResource(R.string.application_name),
          style = MaterialTheme.typography.headlineSmall,
          color = Color.White,
          fontWeight = FontWeight.Bold
        )
      }

      Spacer(modifier = Modifier.height(16.dp))

      // Connection status with improved design
      ConnectionStatusCard(
        connectionState = connectionStatus,
        onConnectionClick = onConnectionToggle,
        isDarkTheme = isDarkTheme
      )
    }
  }
}

/**
 * Connection status card with interactive design.
 */
@Composable
private fun ConnectionStatusCard(
  connectionState: ConnectionStatus,
  onConnectionClick: () -> Unit,
  isDarkTheme: Boolean = false
) {
  val (statusText, statusColor, statusIcon) = when (connectionState) {
    ConnectionStatus.Connected -> Triple(
      stringResource(R.string.drawer_connection_status_active),
      connection_status_connected,
      Icons.Default.Wifi
    )
    ConnectionStatus.Authenticating -> Triple(
      stringResource(R.string.drawer_connection_status_on),
      MaterialTheme.colorScheme.secondary,
      Icons.Default.Circle
    )
    ConnectionStatus.Offline -> Triple(
      stringResource(R.string.drawer_connection_status_off),
      connection_status_offline,
      Icons.Default.WifiOff
    )
  }

  Card(
    modifier = Modifier
      .fillMaxWidth()
      .heightIn(min = 44.dp)
      .clickable { onConnectionClick() },
    colors = CardDefaults.cardColors(
      containerColor = if (isDarkTheme) {
        connection_status_card_bg_dark
      } else {
        connection_status_card_bg_light
      }
    ),
    shape = RoundedCornerShape(8.dp),
    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
  ) {
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 16.dp, vertical = 12.dp),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
      Icon(
        imageVector = statusIcon,
        contentDescription = null,
        tint = statusColor,
        modifier = Modifier.size(20.dp)
      )

      Text(
        text = statusText,
        style = MaterialTheme.typography.bodyMedium,
        color = if (isDarkTheme) Color.White else Color.Black,
        fontWeight = FontWeight.Medium
      )
    }
  }
}

/**
 * Reusable navigation drawer item composable
 */
@Composable
private fun DrawerNavigationItem(
  item: DrawerItem,
  currentRoute: String?,
  onNavigate: (Screen) -> Unit,
  colors: NavigationDrawerItemColors = NavigationDrawerItemDefaults.colors(
    selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
    selectedIconColor = MaterialTheme.colorScheme.primary,
    selectedTextColor = MaterialTheme.colorScheme.primary
  ),
  onClick: (() -> Unit)? = null
) {
  NavigationDrawerItem(
    icon = {
      Icon(
        imageVector = item.icon,
        contentDescription = null
      )
    },
    label = {
      Text(
        text = stringResource(item.titleRes),
        style = MaterialTheme.typography.bodyLarge
      )
    },
    selected = currentRoute == item.screen.route,
    onClick = onClick ?: { onNavigate(item.screen) },
    modifier = Modifier.padding(horizontal = 16.dp),
    colors = colors
  )
}

/**
 * Main navigation items in the drawer with improved grouping.
 */
@Composable
private fun DrawerNavigationItems(currentRoute: String?, onNavigate: (Screen) -> Unit) {
  // Primary navigation items
  val primaryItems = listOf(
    DrawerItem(Screen.Home, Icons.Default.Home, R.string.nav_now_playing),
    DrawerItem(Screen.NowPlayingList, Icons.AutoMirrored.Filled.QueueMusic, R.string.nav_queue),
    DrawerItem(Screen.Library, Icons.Default.LibraryMusic, R.string.nav_library),
    DrawerItem(Screen.Playlists, Icons.AutoMirrored.Filled.PlaylistPlay, R.string.nav_playlists),
    DrawerItem(Screen.Radio, Icons.Default.Radio, R.string.nav_radio)
  )

  // Secondary navigation items
  val secondaryItems = listOf(
    DrawerItem(Screen.Settings, Icons.Default.Settings, R.string.nav_settings),
    DrawerItem(Screen.Help, Icons.AutoMirrored.Filled.Help, R.string.nav_help)
  )

  Column(modifier = Modifier.padding(vertical = 8.dp)) {
    // Primary navigation section
    primaryItems.forEach { item ->
      DrawerNavigationItem(
        item = item,
        currentRoute = currentRoute,
        onNavigate = onNavigate
      )
    }

    // Divider between sections
    HorizontalDivider(
      modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
      color = MaterialTheme.colorScheme.outlineVariant
    )

    // Secondary navigation section
    secondaryItems.forEach { item ->
      DrawerNavigationItem(
        item = item,
        currentRoute = currentRoute,
        onNavigate = onNavigate
      )
    }
  }
}

/**
 * Data class representing a drawer navigation item.
 */
private data class DrawerItem(val screen: Screen, val icon: ImageVector, val titleRes: Int)
