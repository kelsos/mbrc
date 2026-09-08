package com.kelsos.mbrc.ui

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Help
import androidx.compose.material.icons.automirrored.filled.PlaylistPlay
import androidx.compose.material.icons.automirrored.filled.QueueMusic
import androidx.compose.material.icons.filled.DesktopWindows
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.filled.Radio
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material.icons.filled.WifiOff
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DrawerState
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.NavigationDrawerItemColors
import androidx.compose.material3.NavigationDrawerItemDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import androidx.navigation.compose.currentBackStackEntryAsState
import com.kelsos.mbrc.BuildConfig
import com.kelsos.mbrc.R
import com.kelsos.mbrc.core.common.state.ConnectionStatus
import com.kelsos.mbrc.core.ui.theme.connection_status_connected
import com.kelsos.mbrc.core.ui.theme.connection_status_connecting
import com.kelsos.mbrc.core.ui.theme.connection_status_offline
import com.kelsos.mbrc.core.ui.theme.drawer_header_gradient_top_dark
import com.kelsos.mbrc.core.ui.theme.drawer_header_gradient_top_light
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
  modifier: Modifier = Modifier,
  onRequestLocalNetworkAccess: () -> Unit = {}
) {
  val currentBackStackEntry by navController.currentBackStackEntryAsState()
  val currentRoute = currentBackStackEntry?.destination?.route
  val scope = rememberCoroutineScope()
  val connectionStatus by drawerViewModel.connectionStatus.collectAsStateWithLifecycle()
  val connectionName by drawerViewModel.connectionName.collectAsStateWithLifecycle()

  // Tapping connect while local network access is denied asks for access instead of starting a
  // connection attempt that cannot succeed.
  val onConnectionToggle = remember(drawerViewModel, onRequestLocalNetworkAccess) {
    {
      if (!drawerViewModel.toggleConnection()) {
        onRequestLocalNetworkAccess()
      }
    }
  }

  val onNavigate: (Screen) -> Unit = remember(scope, drawerState, navController) {
    { screen: Screen ->
      // Navigate on the tap instead of awaiting the close animation first. A navigation that
      // lands a few hundred milliseconds later can arrive in the middle of a predictive back
      // gesture, and the pop that gesture then performs targets an entry that is no longer the
      // top of the back stack, which crashes (#348). The guard covers the residual case where
      // a gesture is already under way when the item is tapped.
      val current = navController.currentBackStackEntry
      if (current == null || current.lifecycleIsResumed()) {
        navController.navigate(screen.route) {
          popUpTo(navController.graph.startDestinationId) {
            saveState = true
          }
          launchSingleTop = true
          restoreState = true
        }
      }
      scope.launch { drawerState.close() }
    }
  }

  DrawerContent(
    currentRoute = currentRoute,
    connectionStatus = connectionStatus,
    connectionName = connectionName,
    versionName = BuildConfig.VERSION_NAME,
    onConnectionToggle = onConnectionToggle,
    onNavigate = onNavigate,
    modifier = modifier
  )
}

/**
 * Stateless drawer content that can be used in previews and tests.
 */
@Composable
fun DrawerContent(
  currentRoute: String?,
  connectionStatus: ConnectionStatus,
  connectionName: String?,
  versionName: String,
  onConnectionToggle: () -> Unit,
  onNavigate: (Screen) -> Unit,
  modifier: Modifier = Modifier
) {
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
        connectionName = connectionName,
        onConnectionToggle = onConnectionToggle
      )

      // Main navigation items
      DrawerNavigationItems(
        currentRoute = currentRoute,
        onNavigate = onNavigate
      )

      Spacer(modifier = Modifier.weight(1f))

      // Version info
      Text(
        text = stringResource(R.string.drawer_version, versionName),
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
        modifier = Modifier
          .fillMaxWidth()
          .padding(horizontal = 28.dp, vertical = 16.dp)
      )
    }
  }
}

/**
 * Header section of the drawer showing app branding and connection status.
 * Compact design with icon button for connection status.
 */
@Composable
private fun DrawerHeader(
  connectionStatus: ConnectionStatus,
  connectionName: String?,
  onConnectionToggle: () -> Unit
) {
  val isDarkTheme = isSystemInDarkTheme()

  // Subtler gradient - less contrast between top and bottom
  val gradientColors = remember(isDarkTheme) {
    if (isDarkTheme) {
      listOf(
        drawer_header_gradient_top_dark.copy(alpha = 0.9f),
        drawer_header_gradient_top_dark.copy(alpha = 0.7f)
      )
    } else {
      listOf(
        drawer_header_gradient_top_light,
        drawer_header_gradient_top_light.copy(alpha = 0.85f)
      )
    }
  }

  Box(
    modifier = Modifier
      .fillMaxWidth()
      .height(130.dp)
      .background(Brush.verticalGradient(colors = gradientColors))
  ) {
    Column(
      modifier = Modifier
        .fillMaxSize()
        .padding(horizontal = 20.dp, vertical = 16.dp),
      verticalArrangement = Arrangement.Bottom
    ) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
      ) {
        // App logo and name
        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
          // App icon with rounded container
          Surface(
            modifier = Modifier.size(48.dp),
            shape = RoundedCornerShape(12.dp),
            color = Color.White.copy(alpha = 0.2f)
          ) {
            Box(
              modifier = Modifier.fillMaxSize(),
              contentAlignment = Alignment.Center
            ) {
              Image(
                painter = painterResource(id = R.mipmap.ic_launcher),
                contentDescription = null,
                modifier = Modifier.size(36.dp)
              )
            }
          }

          // App name and connection info
          Column {
            Text(
              text = stringResource(R.string.application_name),
              style = MaterialTheme.typography.titleMedium,
              color = Color.White,
              fontWeight = FontWeight.SemiBold
            )
            // Show connection status or name
            val statusText = when (connectionStatus) {
              is ConnectionStatus.Connected -> connectionName

              is ConnectionStatus.Connecting -> {
                val cycle = connectionStatus.cycle
                if (cycle != null) {
                  stringResource(
                    R.string.drawer_connection_connecting_cycle,
                    cycle,
                    connectionStatus.maxCycles
                  )
                } else {
                  stringResource(R.string.drawer_connection_connecting)
                }
              }

              is ConnectionStatus.Authenticating ->
                stringResource(R.string.drawer_connection_status_on)

              is ConnectionStatus.Offline ->
                stringResource(R.string.drawer_connection_not_connected)

              is ConnectionStatus.LocalNetworkDenied ->
                stringResource(R.string.drawer_connection_local_network_denied)
            }
            if (statusText != null) {
              Text(
                text = statusText,
                style = MaterialTheme.typography.bodySmall,
                color = Color.White.copy(alpha = 0.7f)
              )
            }
          }
        }

        // Connection status icon button
        ConnectionStatusIconButton(
          connectionState = connectionStatus,
          onConnectionClick = onConnectionToggle
        )
      }
    }
  }
}

/**
 * Colour and icon for a connection state. Denied local network access is drawn like Offline: the
 * distinction is carried by the label and the banner, not by inventing a third indicator style.
 */
private fun ConnectionStatus.appearance(): Pair<Color, ImageVector> = when (this) {
  ConnectionStatus.Connected -> connection_status_connected to Icons.Default.Wifi
  is ConnectionStatus.Connecting -> connection_status_connecting to Icons.Default.Wifi
  ConnectionStatus.Authenticating -> connection_status_connecting to Icons.Default.Wifi
  ConnectionStatus.Offline -> connection_status_offline to Icons.Default.WifiOff
  ConnectionStatus.LocalNetworkDenied -> connection_status_offline to Icons.Default.WifiOff
}

private fun ConnectionStatus.contentDescriptionRes(): Int = when (this) {
  ConnectionStatus.Connected -> R.string.drawer_connection_status_active
  is ConnectionStatus.Connecting -> R.string.drawer_connection_status_on
  ConnectionStatus.Authenticating -> R.string.drawer_connection_status_on
  ConnectionStatus.Offline -> R.string.drawer_connection_status_off
  ConnectionStatus.LocalNetworkDenied -> R.string.drawer_connection_local_network_denied
}

/**
 * Connection status button with circular progress indicator.
 * Shows cycle progress as an arc and indeterminate spinner when connecting.
 */
@Composable
private fun ConnectionStatusIconButton(
  connectionState: ConnectionStatus,
  onConnectionClick: () -> Unit
) {
  val (statusColor, statusIcon) = connectionState.appearance()

  val isConnecting = connectionState is ConnectionStatus.Connecting ||
    connectionState is ConnectionStatus.Authenticating

  Box(
    modifier = Modifier
      .size(48.dp)
      .clip(CircleShape)
      .clickable { onConnectionClick() },
    contentAlignment = Alignment.Center
  ) {
    // Background circle
    Surface(
      modifier = Modifier.fillMaxSize(),
      shape = CircleShape,
      color = Color.White.copy(alpha = 0.2f)
    ) {}

    // Indeterminate spinner when connecting (outer ring)
    if (isConnecting) {
      ConnectingSpinner(color = statusColor)
    }

    // Cycle progress arc (determinate) - inner ring, only show when we have cycle info
    if (connectionState is ConnectionStatus.Connecting) {
      val cycle = connectionState.cycle
      if (cycle != null) {
        val progress = cycle.toFloat() / connectionState.maxCycles.toFloat()
        CircularProgressIndicator(
          progress = { progress },
          modifier = Modifier.size(36.dp),
          color = statusColor.copy(alpha = 0.7f),
          strokeWidth = 3.dp,
          trackColor = statusColor.copy(alpha = 0.2f)
        )
      }
    }

    // Center icon
    Icon(
      imageVector = statusIcon,
      contentDescription = stringResource(connectionState.contentDescriptionRes()),
      tint = statusColor,
      modifier = Modifier.size(20.dp)
    )
  }
}

/**
 * The ring that turns while a connection is being established.
 *
 * Hand-drawn rather than a Material indeterminate indicator because this is the app's longest
 * running animation: a retry cycle keeps it on screen for as long as MusicBee is unreachable. The
 * rotation is read inside the draw lambda, so a frame of it never reaches composition or layout.
 */
@Composable
private fun ConnectingSpinner(color: Color, modifier: Modifier = Modifier) {
  val transition = rememberInfiniteTransition(label = "connecting")
  val angle = transition.animateFloat(
    initialValue = 0f,
    targetValue = 360f,
    animationSpec = infiniteRepeatable(
      animation = tween(durationMillis = 1_000, easing = LinearEasing)
    ),
    label = "connecting_angle"
  )

  Canvas(modifier = modifier.size(46.dp)) {
    rotate(angle.value) {
      drawArc(
        color = color,
        startAngle = 0f,
        sweepAngle = 90f,
        useCenter = false,
        style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round)
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
    selectedContainerColor = Color.Transparent,
    selectedIconColor = MaterialTheme.colorScheme.primary,
    selectedTextColor = MaterialTheme.colorScheme.onSurface,
    unselectedContainerColor = Color.Transparent,
    unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
    unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
  ),
  onClick: (() -> Unit)? = null
) {
  val isSelected = currentRoute == item.screen.route

  NavigationDrawerItem(
    icon = {
      Icon(
        imageVector = item.icon,
        contentDescription = null,
        modifier = Modifier.size(24.dp)
      )
    },
    label = {
      Text(
        text = stringResource(item.titleRes),
        style = MaterialTheme.typography.labelLarge,
        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
      )
    },
    selected = isSelected,
    onClick = onClick ?: { onNavigate(item.screen) },
    modifier = Modifier
      .fillMaxWidth()
      .padding(horizontal = 12.dp, vertical = 2.dp),
    shape = RoundedCornerShape(28.dp),
    colors = colors
  )
}

/**
 * Section label for navigation groups.
 */
@Composable
private fun SectionLabel(textRes: Int) {
  Text(
    text = stringResource(textRes).uppercase(),
    style = MaterialTheme.typography.labelSmall,
    color = MaterialTheme.colorScheme.onSurfaceVariant,
    fontWeight = FontWeight.Medium,
    modifier = Modifier.padding(horizontal = 28.dp, vertical = 8.dp)
  )
}

/**
 * Main navigation items in the drawer with improved grouping.
 */
@Composable
private fun DrawerNavigationItems(currentRoute: String?, onNavigate: (Screen) -> Unit) {
  Column(modifier = Modifier.padding(vertical = 4.dp)) {
    // Music section label
    SectionLabel(R.string.drawer_section_music)

    // Primary navigation section
    primaryNavigationItems.forEach { item ->
      DrawerNavigationItem(
        item = item,
        currentRoute = currentRoute,
        onNavigate = onNavigate
      )
    }

    Spacer(modifier = Modifier.height(16.dp))

    // Settings section label
    SectionLabel(R.string.common_settings)

    // Secondary navigation section
    secondaryNavigationItems.forEach { item ->
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

// Static navigation item lists - defined outside composable to avoid recreation
private val primaryNavigationItems = listOf(
  DrawerItem(Screen.Home, Icons.Default.Home, R.string.nav_now_playing),
  DrawerItem(Screen.NowPlayingList, Icons.AutoMirrored.Filled.QueueMusic, R.string.nav_queue),
  DrawerItem(Screen.Library, Icons.Default.LibraryMusic, R.string.common_library),
  DrawerItem(Screen.Playlists, Icons.AutoMirrored.Filled.PlaylistPlay, R.string.nav_playlists),
  DrawerItem(Screen.Radio, Icons.Default.Radio, R.string.nav_radio)
)

private val secondaryNavigationItems = listOf(
  DrawerItem(Screen.ConnectionManager, Icons.Default.DesktopWindows, R.string.nav_connections),
  DrawerItem(Screen.Settings, Icons.Default.Settings, R.string.common_settings),
  DrawerItem(Screen.Help, Icons.AutoMirrored.Filled.Help, R.string.nav_help)
)
