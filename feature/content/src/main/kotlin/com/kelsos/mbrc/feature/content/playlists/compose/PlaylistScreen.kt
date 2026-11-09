package com.kelsos.mbrc.feature.content.playlists.compose

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.QueueMusic
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.paging.compose.collectAsLazyPagingItems
import com.kelsos.mbrc.core.data.playlist.PlaylistBrowserItem
import com.kelsos.mbrc.core.ui.R as CoreUiR
import com.kelsos.mbrc.core.ui.compose.NavigationIconType
import com.kelsos.mbrc.core.ui.compose.ScreenScaffold
import com.kelsos.mbrc.core.ui.compose.SingleLineRow
import com.kelsos.mbrc.core.ui.compose.SwipeRefreshScreen
import com.kelsos.mbrc.feature.content.R
import com.kelsos.mbrc.feature.content.playlists.PlaylistUiMessages
import com.kelsos.mbrc.feature.content.playlists.PlaylistViewModel
import com.kelsos.mbrc.feature.content.playlists.getPathDisplayName
import com.kelsos.mbrc.feature.minicontrol.MiniControl
import org.koin.androidx.compose.koinViewModel

@Composable
fun PlaylistScreen(
  onNavigateToPlayer: () -> Unit,
  snackbarHostState: SnackbarHostState,
  onOpenDrawer: () -> Unit,
  modifier: Modifier = Modifier,
  viewModel: PlaylistViewModel = koinViewModel()
) {
  val items = viewModel.items.collectAsLazyPagingItems()
  val currentPath by viewModel.currentPath.collectAsStateWithLifecycle()
  var isRefreshing by remember { mutableStateOf(false) }

  val refreshFailedMessage = stringResource(R.string.playlists_load_failed)
  val refreshSuccessMessage = stringResource(R.string.playlists_load_success)
  val networkUnavailableMessage =
    stringResource(CoreUiR.string.connection_error_network_unavailable)
  val playFailedMessage = stringResource(R.string.radio__play_failed)
  val baseTitle = stringResource(R.string.nav_playlists)

  // Dynamic title based on current path
  val title = if (currentPath.isEmpty()) {
    baseTitle
  } else {
    getPathDisplayName(currentPath)
  }

  // Navigation icon: back arrow when in folder, drawer when at root
  val navigationIcon = if (currentPath.isEmpty()) {
    NavigationIconType.Drawer
  } else {
    NavigationIconType.Back { viewModel.actions.navigateUp() }
  }

  // Handle system back navigation
  BackHandler(enabled = currentPath.isNotEmpty()) {
    viewModel.actions.navigateUp()
  }

  LaunchedEffect(Unit) {
    // Trigger initial load without user message
    viewModel.actions.reload(showUserMessage = false)
  }

  LaunchedEffect(Unit) {
    viewModel.events.collect { event ->
      val message = when (event) {
        is PlaylistUiMessages.RefreshFailed -> {
          isRefreshing = false
          refreshFailedMessage
        }

        is PlaylistUiMessages.RefreshSuccess -> {
          isRefreshing = false
          refreshSuccessMessage
        }

        is PlaylistUiMessages.NetworkUnavailable -> {
          isRefreshing = false
          networkUnavailableMessage
        }

        is PlaylistUiMessages.PlayFailed -> playFailedMessage
      }

      snackbarHostState.showSnackbar(
        message = message,
        duration = SnackbarDuration.Short
      )
    }
  }

  ScreenScaffold(
    title = title,
    snackbarHostState = snackbarHostState,
    navigationIcon = navigationIcon,
    onOpenDrawer = onOpenDrawer,
    modifier = modifier
  ) { paddingValues ->
    Column(modifier = Modifier.fillMaxSize().padding(paddingValues)) {
      SwipeRefreshScreen(
        items = items,
        listId = "playlists",
        isRefreshing = isRefreshing,
        onRefresh = {
          isRefreshing = true
          viewModel.actions.reload()
        },
        modifier = Modifier.weight(1f),
        emptyMessage = stringResource(R.string.playlists_list_empty),
        emptyIcon = Icons.AutoMirrored.Filled.QueueMusic,
        key = { it.key }
      ) { item ->
        BrowserItemRow(
          item = item,
          onFolderClick = { viewModel.actions.navigateToFolder(it) },
          onPlaylistClick = { viewModel.actions.play(it) }
        )
      }

      MiniControl(
        onNavigateToPlayer = onNavigateToPlayer,
        snackbarHostState = snackbarHostState
      )
    }
  }
}

@Composable
fun BrowserItemRow(
  item: PlaylistBrowserItem,
  onFolderClick: (path: String) -> Unit,
  onPlaylistClick: (url: String) -> Unit,
  modifier: Modifier = Modifier
) {
  if (item.isFolder) {
    SingleLineRow(
      text = item.name,
      onClick = { onFolderClick(item.path) },
      modifier = modifier,
      leadingContent = {
        Icon(
          imageVector = Icons.Default.Folder,
          contentDescription = null,
          modifier = Modifier.size(24.dp),
          tint = MaterialTheme.colorScheme.secondary
        )
      }
    )
  } else {
    SingleLineRow(
      text = item.name,
      onClick = { onPlaylistClick(item.path) },
      modifier = modifier,
      leadingContent = {
        Icon(
          imageVector = Icons.AutoMirrored.Filled.QueueMusic,
          contentDescription = null,
          modifier = Modifier.size(24.dp),
          tint = MaterialTheme.colorScheme.primary
        )
      },
      trailingContent = {
        IconButton(onClick = { onPlaylistClick(item.path) }) {
          Icon(
            imageVector = Icons.Default.PlayArrow,
            contentDescription = stringResource(CoreUiR.string.action_play),
            tint = MaterialTheme.colorScheme.primary
          )
        }
      }
    )
  }
}
