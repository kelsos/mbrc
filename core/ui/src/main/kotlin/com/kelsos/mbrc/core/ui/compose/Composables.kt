package com.kelsos.mbrc.core.ui.compose

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.paging.LoadState
import androidx.paging.compose.LazyPagingItems
import com.kelsos.mbrc.core.common.layout.WindowWidthClass
import com.kelsos.mbrc.core.ui.R
import com.kelsos.mbrc.core.ui.layout.windowWidthClass

/**
 * Smallest album grid cell for the current window.
 *
 * A fixed 120dp minimum is a phone number: on a 1280dp tablet it fills the screen with ten columns
 * of thumbnails rather than making the covers bigger, which is the wrong trade for a grid whose
 * whole point is the artwork.
 */
@Composable
@ReadOnlyComposable
fun albumGridMinCellSize(): Dp = when (windowWidthClass()) {
  WindowWidthClass.Compact -> 120.dp
  WindowWidthClass.Medium -> 160.dp
  WindowWidthClass.Expanded -> 200.dp
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RemoteTopAppBar(
  title: String,
  navigationIcon: @Composable (() -> Unit)? = null,
  actions: @Composable () -> Unit = {},
  transparent: Boolean = false
) {
  TopAppBar(
    title = {
      Text(
        text = title,
        style = MaterialTheme.typography.titleLarge
      )
    },
    navigationIcon = {
      navigationIcon?.invoke()
    },
    actions = { actions() },
    colors = if (transparent) {
      TopAppBarDefaults.topAppBarColors(
        containerColor = Color.Transparent,
        scrolledContainerColor = Color.Transparent
      )
    } else {
      TopAppBarDefaults.topAppBarColors()
    }
  )
}

@Composable
fun DrawerNavigationIcon(onClick: () -> Unit) {
  IconButton(onClick = onClick) {
    Icon(
      imageVector = Icons.Default.Menu,
      contentDescription = stringResource(R.string.drawer_open)
    )
  }
}

@Composable
fun BackNavigationIcon(onClick: () -> Unit) {
  IconButton(onClick = onClick) {
    Icon(
      imageVector = Icons.AutoMirrored.Filled.ArrowBack,
      contentDescription = stringResource(R.string.navigate_back)
    )
  }
}

@Composable
fun EmptyScreen(
  message: String,
  modifier: Modifier = Modifier,
  icon: ImageVector? = null,
  contentColor: Color = MaterialTheme.colorScheme.onSurfaceVariant
) {
  Box(
    modifier = modifier.fillMaxSize(),
    contentAlignment = Alignment.Center
  ) {
    Column(
      horizontalAlignment = Alignment.CenterHorizontally,
      verticalArrangement = Arrangement.Center
    ) {
      icon?.let {
        Icon(
          imageVector = it,
          contentDescription = null,
          modifier = Modifier.size(72.dp),
          tint = contentColor
        )
        Spacer(modifier = Modifier.size(16.dp))
      }
      Text(
        text = message,
        style = MaterialTheme.typography.bodyLarge,
        color = contentColor,
        textAlign = TextAlign.Center
      )
    }
  }
}

@Composable
fun LoadingScreen(modifier: Modifier = Modifier, message: String? = null) {
  val isInPreview = LocalInspectionMode.current
  Box(
    modifier = modifier.fillMaxSize(),
    contentAlignment = Alignment.Center
  ) {
    Column(
      horizontalAlignment = Alignment.CenterHorizontally,
      verticalArrangement = Arrangement.Center
    ) {
      if (isInPreview) {
        CircularProgressIndicator(progress = { 0.7f })
      } else {
        CircularProgressIndicator()
      }
      message?.let {
        Spacer(modifier = Modifier.size(16.dp))
        Text(
          text = it,
          style = MaterialTheme.typography.bodyMedium,
          color = MaterialTheme.colorScheme.onSurfaceVariant
        )
      }
    }
  }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun SingleLineRow(
  text: String,
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
  onLongClick: (() -> Unit)? = null,
  fontWeight: FontWeight? = null,
  leadingContent: @Composable (() -> Unit)? = null,
  trailingContent: @Composable (() -> Unit)? = null
) {
  ListItem(
    headlineContent = {
      Text(
        text = text,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        fontWeight = fontWeight
      )
    },
    leadingContent = leadingContent,
    trailingContent = trailingContent,
    modifier = if (onLongClick != null) {
      modifier.combinedClickable(
        onClick = onClick,
        onLongClick = onLongClick
      )
    } else {
      modifier.clickable(onClick = onClick)
    }
  )
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun DoubleLineRow(
  title: String,
  subtitle: String,
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
  onLongClick: (() -> Unit)? = null,
  leadingContent: @Composable (() -> Unit)? = null,
  trailingContent: @Composable (() -> Unit)? = null
) {
  ListItem(
    headlineContent = {
      Text(
        text = title,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis
      )
    },
    supportingContent = {
      Text(
        text = subtitle,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis
      )
    },
    leadingContent = leadingContent,
    trailingContent = trailingContent,
    modifier = if (onLongClick != null) {
      modifier.combinedClickable(
        onClick = onClick,
        onLongClick = onLongClick
      )
    } else {
      modifier.clickable(onClick = onClick)
    }
  )
}

@Composable
fun PopupMenu(expanded: Boolean, onDismiss: () -> Unit, items: List<PopupMenuItem>) {
  DropdownMenu(
    expanded = expanded,
    onDismissRequest = onDismiss
  ) {
    items.forEach { item ->
      DropdownMenuItem(
        text = { Text(item.title) },
        onClick = {
          onDismiss()
          item.onClick()
        },
        leadingIcon = item.icon?.let {
          {
            Icon(
              imageVector = it,
              contentDescription = null
            )
          }
        }
      )
    }
  }
}

data class PopupMenuItem(val title: String, val icon: ImageVector? = null, val onClick: () -> Unit)

@Composable
fun MoreOptionsButton(
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
  contentDescription: String = stringResource(R.string.menu_overflow_description)
) {
  IconButton(
    onClick = onClick,
    modifier = modifier.size(48.dp)
  ) {
    Icon(
      imageVector = Icons.Default.MoreVert,
      contentDescription = contentDescription
    )
  }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun <T : Any> SwipeRefreshScreen(
  items: LazyPagingItems<T>,
  listId: String,
  isRefreshing: Boolean,
  onRefresh: () -> Unit,
  modifier: Modifier = Modifier,
  emptyMessage: String = stringResource(R.string.no_data),
  emptyIcon: ImageVector? = null,
  key: ((T) -> Any)? = null,
  itemContent: @Composable (T) -> Unit
) {
  PullToRefreshBox(
    isRefreshing = isRefreshing,
    onRefresh = onRefresh,
    modifier = modifier
  ) {
    when (items.loadState.refresh) {
      is LoadState.Loading if items.itemCount == 0 -> {
        LoadingScreen()
      }

      is LoadState.NotLoading if items.itemCount == 0 -> {
        BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
          EmptyScreen(
            message = emptyMessage,
            icon = emptyIcon,
            modifier = Modifier
              .fillMaxWidth()
              .heightIn(min = maxHeight)
              .verticalScroll(rememberScrollState())
          )
        }
      }

      else -> {
        LazyColumn(
          modifier = Modifier.fillMaxSize()
        ) {
          items(
            count = items.itemCount,
            key = key?.let { keyFunc -> pagedItemKey(listId, items, keyFunc) },
            contentType = { "list_item" }
          ) { index ->
            items[index]?.let { item ->
              itemContent(item)
            }
          }

          if (items.loadState.append is LoadState.Loading) {
            item(contentType = "loading") {
              Box(
                modifier = Modifier
                  .fillMaxWidth()
                  .padding(16.dp),
                contentAlignment = Alignment.Center
              ) {
                CircularProgressIndicator()
              }
            }
          }
        }
      }
    }
  }
}

/**
 * A simple paging list screen without pull-to-refresh functionality.
 * Use this for drilldown screens where refresh is not needed.
 */
@Composable
fun <T : Any> PagingListScreen(
  items: LazyPagingItems<T>,
  listId: String,
  modifier: Modifier = Modifier,
  emptyMessage: String = stringResource(R.string.no_data),
  emptyIcon: ImageVector? = null,
  key: ((T) -> Any)? = null,
  itemContent: @Composable (T) -> Unit
) {
  when (items.loadState.refresh) {
    is LoadState.Loading if items.itemCount == 0 -> {
      LoadingScreen(modifier = modifier)
    }

    is LoadState.NotLoading if items.itemCount == 0 -> {
      EmptyScreen(
        message = emptyMessage,
        icon = emptyIcon,
        modifier = modifier
      )
    }

    else -> {
      LazyColumn(
        modifier = modifier.fillMaxSize()
      ) {
        items(
          count = items.itemCount,
          key = key?.let { keyFunc -> pagedItemKey(listId, items, keyFunc) },
          contentType = { "list_item" }
        ) { index ->
          items[index]?.let { item ->
            itemContent(item)
          }
        }

        if (items.loadState.append is LoadState.Loading) {
          item(contentType = "loading") {
            Box(
              modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
              contentAlignment = Alignment.Center
            ) {
              CircularProgressIndicator()
            }
          }
        }
      }
    }
  }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun <T : Any> SwipeRefreshGridScreen(
  items: LazyPagingItems<T>,
  listId: String,
  isRefreshing: Boolean,
  onRefresh: () -> Unit,
  modifier: Modifier = Modifier,
  emptyMessage: String = stringResource(R.string.no_data),
  emptyIcon: ImageVector? = null,
  key: ((T) -> Any)? = null,
  itemContent: @Composable (T) -> Unit
) {
  PullToRefreshBox(
    isRefreshing = isRefreshing,
    onRefresh = onRefresh,
    modifier = modifier
  ) {
    when (items.loadState.refresh) {
      is LoadState.Loading if items.itemCount == 0 -> {
        LoadingScreen()
      }

      is LoadState.NotLoading if items.itemCount == 0 -> {
        BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
          EmptyScreen(
            message = emptyMessage,
            icon = emptyIcon,
            modifier = Modifier
              .fillMaxWidth()
              .heightIn(min = maxHeight)
              .verticalScroll(rememberScrollState())
          )
        }
      }

      else -> {
        LazyVerticalGrid(
          columns = GridCells.Adaptive(minSize = albumGridMinCellSize()),
          modifier = Modifier.fillMaxSize(),
          contentPadding = PaddingValues(8.dp),
          horizontalArrangement = Arrangement.spacedBy(8.dp),
          verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
          items(
            count = items.itemCount,
            key = key?.let { keyFunc -> pagedItemKey(listId, items, keyFunc) },
            contentType = { "grid_item" }
          ) { index ->
            items[index]?.let { item ->
              itemContent(item)
            }
          }

          if (items.loadState.append is LoadState.Loading) {
            item(contentType = "loading") {
              Box(
                modifier = Modifier
                  .fillMaxWidth()
                  .padding(16.dp),
                contentAlignment = Alignment.Center
              ) {
                CircularProgressIndicator()
              }
            }
          }
        }
      }
    }
  }
}

@Composable
fun <T : Any> PagingGridScreen(
  items: LazyPagingItems<T>,
  listId: String,
  modifier: Modifier = Modifier,
  emptyMessage: String = stringResource(R.string.no_data),
  emptyIcon: ImageVector? = null,
  key: ((T) -> Any)? = null,
  itemContent: @Composable (T) -> Unit
) {
  when (items.loadState.refresh) {
    is LoadState.Loading if items.itemCount == 0 -> {
      LoadingScreen(modifier = modifier)
    }

    is LoadState.NotLoading if items.itemCount == 0 -> {
      EmptyScreen(
        message = emptyMessage,
        icon = emptyIcon,
        modifier = modifier
      )
    }

    else -> {
      LazyVerticalGrid(
        columns = GridCells.Adaptive(minSize = albumGridMinCellSize()),
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
      ) {
        items(
          count = items.itemCount,
          key = key?.let { keyFunc -> pagedItemKey(listId, items, keyFunc) },
          contentType = { "grid_item" }
        ) { index ->
          items[index]?.let { item ->
            itemContent(item)
          }
        }

        if (items.loadState.append is LoadState.Loading) {
          item(contentType = "loading") {
            Box(
              modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
              contentAlignment = Alignment.Center
            ) {
              CircularProgressIndicator()
            }
          }
        }
      }
    }
  }
}

@Composable
fun ErrorScreen(message: String, modifier: Modifier = Modifier, onRetry: (() -> Unit)? = null) {
  Box(
    modifier = modifier.fillMaxSize(),
    contentAlignment = Alignment.Center
  ) {
    Column(
      horizontalAlignment = Alignment.CenterHorizontally,
      verticalArrangement = Arrangement.Center
    ) {
      Text(
        text = message,
        style = MaterialTheme.typography.bodyLarge,
        color = MaterialTheme.colorScheme.error,
        textAlign = TextAlign.Center
      )
      onRetry?.let {
        Spacer(modifier = Modifier.size(16.dp))
        TextButton(onClick = it) {
          Text(stringResource(R.string.action_retry))
        }
      }
    }
  }
}
