package com.kelsos.mbrc.core.ui.compose

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.kelsos.mbrc.core.ui.R
import com.kelsos.mbrc.core.ui.layout.windowWidthClass

/**
 * Main scaffold top bar that renders different UI based on [TopBarState].
 *
 * @param state The current top bar state
 * @param navigationIcon The type of navigation icon to display
 * @param actionItems Action icon buttons shown before the overflow menu
 * @param menuItems Menu items for the overflow menu
 * @param isTransparent Whether the top bar should be transparent
 * @param defaultTitle Title to show when state is [TopBarState.Default]
 * @param onOpenDrawer Callback to open the navigation drawer
 * @param onOverflowClick Optional callback that overrides the menu dropdown behavior
 */
@Composable
fun ScaffoldTopBar(
  state: TopBarState,
  navigationIcon: NavigationIconType,
  actionItems: List<ActionItem>,
  menuItems: List<MenuItem>,
  isTransparent: Boolean,
  defaultTitle: String,
  onOpenDrawer: () -> Unit,
  onOverflowClick: (() -> Unit)? = null
) {
  when (state) {
    TopBarState.Default -> DefaultTopBar(
      title = defaultTitle,
      navigationIcon = navigationIcon,
      actionItems = actionItems,
      menuItems = menuItems,
      isTransparent = isTransparent,
      onOpenDrawer = onOpenDrawer,
      onOverflowClick = onOverflowClick
    )

    TopBarState.Hidden -> {
      // Render nothing
    }

    is TopBarState.WithTitle -> DefaultTopBar(
      title = state.title,
      navigationIcon = navigationIcon,
      actionItems = actionItems,
      menuItems = menuItems,
      isTransparent = isTransparent,
      onOpenDrawer = onOpenDrawer,
      onOverflowClick = onOverflowClick
    )

    is TopBarState.Search -> SearchTopBar(
      query = state.query,
      placeholder = state.placeholder,
      onQueryChange = state.onQueryChange,
      onSearch = state.onSearch,
      onClose = state.onClose
    )

    is TopBarState.WithProgress -> ProgressTopBar(
      title = state.title,
      progress = state.progress,
      progressText = state.progressText,
      navigationIcon = navigationIcon,
      actionItems = actionItems,
      menuItems = menuItems,
      onOpenDrawer = onOpenDrawer,
      onOverflowClick = onOverflowClick
    )
  }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DefaultTopBar(
  title: String,
  navigationIcon: NavigationIconType,
  actionItems: List<ActionItem>,
  menuItems: List<MenuItem>,
  isTransparent: Boolean,
  onOpenDrawer: () -> Unit,
  onOverflowClick: (() -> Unit)? = null
) {
  TopAppBar(
    title = {
      Text(
        text = title,
        style = MaterialTheme.typography.titleLarge
      )
    },
    navigationIcon = {
      NavigationIconContent(navigationIcon, onOpenDrawer)
    },
    actions = {
      // Render action items first
      actionItems.forEach { action ->
        IconButton(onClick = action.onClick) {
          Icon(
            imageVector = action.icon,
            contentDescription = action.contentDescription
          )
        }
      }
      // Then the overflow menu
      MenuDropdown(menuItems, onOverflowClick)
    },
    colors = if (isTransparent) {
      TopAppBarDefaults.topAppBarColors(
        containerColor = Color.Transparent,
        scrolledContainerColor = Color.Transparent
      )
    } else {
      TopAppBarDefaults.topAppBarColors()
    }
  )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SearchTopBar(
  query: String,
  placeholder: String,
  onQueryChange: (String) -> Unit,
  onSearch: () -> Unit,
  onClose: () -> Unit
) {
  val keyboardController = LocalSoftwareKeyboardController.current

  TopAppBar(
    title = {
      TextField(
        value = query,
        onValueChange = onQueryChange,
        placeholder = { Text(placeholder) },
        singleLine = true,
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
        keyboardActions = KeyboardActions(
          onSearch = {
            keyboardController?.hide()
            onSearch()
          }
        ),
        colors = TextFieldDefaults.colors(
          focusedContainerColor = Color.Transparent,
          unfocusedContainerColor = Color.Transparent,
          focusedIndicatorColor = Color.Transparent,
          unfocusedIndicatorColor = Color.Transparent
        ),
        modifier = Modifier.fillMaxWidth()
      )
    },
    navigationIcon = {
      IconButton(onClick = onClose) {
        Icon(
          imageVector = Icons.AutoMirrored.Filled.ArrowBack,
          contentDescription = stringResource(R.string.action_search_clear)
        )
      }
    },
    actions = {
      if (query.isNotEmpty()) {
        IconButton(onClick = { onQueryChange("") }) {
          Icon(
            imageVector = Icons.Default.Clear,
            contentDescription = stringResource(R.string.action_search_clear)
          )
        }
      }
    }
  )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ProgressTopBar(
  title: String,
  progress: Float,
  progressText: String?,
  navigationIcon: NavigationIconType,
  actionItems: List<ActionItem>,
  menuItems: List<MenuItem>,
  onOpenDrawer: () -> Unit,
  onOverflowClick: (() -> Unit)? = null
) {
  val animatedProgress by animateFloatAsState(
    targetValue = if (progress >= 0f) progress else 0f,
    label = "progress_animation"
  )

  Column {
    TopAppBar(
      title = {
        Text(
          text = title,
          style = MaterialTheme.typography.titleLarge
        )
      },
      navigationIcon = {
        NavigationIconContent(navigationIcon, onOpenDrawer)
      },
      actions = {
        // Render action items first
        actionItems.forEach { action ->
          IconButton(onClick = action.onClick) {
            Icon(
              imageVector = action.icon,
              contentDescription = action.contentDescription
            )
          }
        }
        // Then the overflow menu
        MenuDropdown(menuItems, onOverflowClick)
      }
    )

    AnimatedVisibility(
      visible = progress >= 0f || progress == -1f,
      enter = fadeIn(),
      exit = fadeOut()
    ) {
      Column(modifier = Modifier.fillMaxWidth()) {
        if (progress == -1f) {
          // Indeterminate progress
          LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
        } else {
          LinearProgressIndicator(
            progress = { animatedProgress },
            modifier = Modifier.fillMaxWidth()
          )
        }
        progressText?.let { text ->
          Text(
            text = text,
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
          )
        }
      }
    }
  }
}

@Composable
private fun NavigationIconContent(navigationIcon: NavigationIconType, onOpenDrawer: () -> Unit) {
  // At medium and expanded widths the destinations are already on screen in a rail or a permanent
  // drawer, so the menu button would open something that is not hidden. Suppressed here rather
  // than at each call site so no screen can forget.
  val navigationIsPersistent = windowWidthClass().prefersPersistentNavigation

  when (navigationIcon) {
    NavigationIconType.Drawer ->
      if (!navigationIsPersistent) {
        DrawerNavigationIcon(onClick = onOpenDrawer)
      }

    is NavigationIconType.Back -> BackNavigationIcon(onClick = navigationIcon.onBack)

    NavigationIconType.None -> { /* No icon */ }
  }
}

@Composable
private fun MenuDropdown(menuItems: List<MenuItem>, onOverflowClick: (() -> Unit)? = null) {
  // If no items and no override, show nothing
  if (menuItems.isEmpty() && onOverflowClick == null) return

  var expanded by remember { mutableStateOf(false) }

  Box {
    IconButton(
      onClick = {
        if (onOverflowClick != null) {
          // Direct action instead of dropdown
          onOverflowClick()
        } else {
          expanded = true
        }
      }
    ) {
      Icon(
        imageVector = Icons.Default.MoreVert,
        contentDescription = stringResource(R.string.menu_overflow_description)
      )
    }

    // Only show dropdown if we have menu items and no override
    if (onOverflowClick == null && menuItems.isNotEmpty()) {
      DropdownMenu(
        expanded = expanded,
        onDismissRequest = { expanded = false }
      ) {
        menuItems.forEach { item ->
          DropdownMenuItem(
            text = { Text(item.label) },
            onClick = {
              expanded = false
              item.onClick()
            },
            trailingIcon = item.trailingContent
          )
        }
      }
    }
  }
}
