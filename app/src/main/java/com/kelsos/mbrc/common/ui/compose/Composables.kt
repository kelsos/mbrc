package com.kelsos.mbrc.common.ui.compose

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.paging.LoadState
import androidx.paging.compose.LazyPagingItems
import com.kelsos.mbrc.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RemoteTopAppBar(
  title: String,
  navigationIcon: @Composable (() -> Unit)? = null,
  actions: @Composable () -> Unit = {}
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
    actions = { actions() }
    // Using default Material 3 top app bar colors which automatically handles surface tint
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
fun EmptyScreen(message: String, modifier: Modifier = Modifier, icon: ImageVector? = null) {
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
          tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.size(16.dp))
      }
      Text(
        text = message,
        style = MaterialTheme.typography.bodyLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        textAlign = TextAlign.Center
      )
    }
  }
}

@Composable
fun LoadingScreen(modifier: Modifier = Modifier, message: String? = null) {
  Box(
    modifier = modifier.fillMaxSize(),
    contentAlignment = Alignment.Center
  ) {
    Column(
      horizontalAlignment = Alignment.CenterHorizontally,
      verticalArrangement = Arrangement.Center
    ) {
      CircularProgressIndicator()
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
  leadingContent: @Composable (() -> Unit)? = null,
  trailingContent: @Composable (() -> Unit)? = null
) {
  ListItem(
    headlineContent = {
      Text(
        text = text,
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
  contentDescription: String = stringResource(R.string.menu_overflow_description)
) {
  IconButton(onClick = onClick) {
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
    when {
      items.loadState.refresh is LoadState.Loading && items.itemCount == 0 -> {
        LoadingScreen()
      }
      items.loadState.refresh is LoadState.NotLoading && items.itemCount == 0 -> {
        EmptyScreen(
          message = emptyMessage,
          icon = emptyIcon
        )
      }
      else -> {
        LazyColumn(
          modifier = Modifier.fillMaxSize()
        ) {
          items(
            count = items.itemCount,
            key = key?.let { keyFunc ->
              { index ->
                items[index]?.let(keyFunc) ?: index
              }
            }
          ) { index ->
            items[index]?.let { item ->
              itemContent(item)
            }
          }

          if (items.loadState.append is LoadState.Loading) {
            item {
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
