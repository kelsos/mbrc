package com.kelsos.mbrc.features.settings.compose

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Computer
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SmallFloatingActionButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.paging.LoadState
import androidx.paging.compose.LazyPagingItems
import androidx.paging.compose.collectAsLazyPagingItems
import com.kelsos.mbrc.R
import com.kelsos.mbrc.app.ConfigurableScreen
import com.kelsos.mbrc.app.ConfigurableScreenContainer
import com.kelsos.mbrc.app.ScreenConfig
import com.kelsos.mbrc.features.settings.ConnectionManagerViewModel
import com.kelsos.mbrc.features.settings.ConnectionSettings
import com.kelsos.mbrc.networking.discovery.DiscoveryStop
import kotlinx.coroutines.flow.map
import org.koin.compose.koinInject

/**
 * Wrapper composable that ensures ConnectionManagerScreen and its config share the same state.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConnectionManagerScreenWithConfig(onScreenConfigChange: (ScreenConfig) -> Unit) {
  val state = rememberConnectionManagerScreenState()
  val config = rememberConnectionManagerScreenConfig(state)

  ConfigurableScreenContainer(
    configurableScreen = config,
    onScreenConfigChange = onScreenConfigChange
  ) {
    ConnectionManagerScreen(state = state)
  }
}

/**
 * Connection Manager screen for managing MusicBee plugin connections.
 * Allows users to add, edit, delete, and scan for connections.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConnectionManagerScreen(
  modifier: Modifier = Modifier,
  state: ConnectionManagerScreenState = rememberConnectionManagerScreenState()
) {
  val viewModel: ConnectionManagerViewModel = koinInject()
  val connections = viewModel.state.settings.collectAsLazyPagingItems()

  LaunchedEffect(viewModel) {
    viewModel.state.events.collect { event ->
      state.stopScanning()
    }
  }

  Box(modifier = modifier.fillMaxSize()) {
    ConnectionList(
      connections = connections,
      onEdit = state::showEditDialog,
      onDelete = state::deleteConnection,
      onSetDefault = state::setDefaultConnection
    )

    if (state.isScanning) {
      Box(
        modifier = Modifier
          .fillMaxSize()
          .background(MaterialTheme.colorScheme.scrim.copy(alpha = 0.6f))
          .clickable(enabled = false) { /* Block clicks */ },
        contentAlignment = Alignment.Center
      ) {
        Card(
          modifier = Modifier
            .wrapContentSize()
            .padding(32.dp),
          colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
          )
        ) {
          Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.padding(24.dp)
          ) {
            CircularProgressIndicator(
              modifier = Modifier.size(48.dp)
            )
            Text(
              text = stringResource(R.string.connection_manager_scanning),
              style = MaterialTheme.typography.bodyLarge,
              color = MaterialTheme.colorScheme.onSurface,
              textAlign = TextAlign.Center
            )
          }
        }
      }
    }
  }

  if (state.showAddDialog || state.editingConnection != null) {
    AddEditConnectionDialog(
      connection = state.editingConnection,
      onDismiss = state::hideDialog,
      onSave = state::saveConnection
    )
  }
}

/**
 * Creates a ConfigurableScreen instance for the ConnectionManagerScreen.
 * This function provides access to the screen's configuration within a Composable context.
 */
@Composable
fun rememberConnectionManagerScreenConfig(state: ConnectionManagerScreenState): ConfigurableScreen {
  val viewModel: ConnectionManagerViewModel = koinInject()
  val context = LocalContext.current

  return remember(viewModel, state, context) {
    ConnectionManagerScreenConfig(viewModel, state, context)
  }
}

/**
 * List of connections with actions.
 */
@Composable
private fun ConnectionList(
  connections: LazyPagingItems<ConnectionSettings>,
  onEdit: (ConnectionSettings) -> Unit,
  onDelete: (ConnectionSettings) -> Unit,
  onSetDefault: (ConnectionSettings) -> Unit
) {
  when (connections.loadState.refresh) {
    is LoadState.Loading -> {
      Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
      ) {
        CircularProgressIndicator()
      }
    }
    is LoadState.Error -> {
      Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
      ) {
        Text(
          text = stringResource(R.string.connection_manager_error_loading),
          style = MaterialTheme.typography.bodyLarge
        )
      }
    }
    is LoadState.NotLoading -> {
      if (connections.itemCount == 0) {
        EmptyConnectionsState()
      } else {
        LazyColumn(
          modifier = Modifier.fillMaxSize(),
          verticalArrangement = Arrangement.spacedBy(8.dp),
          contentPadding = PaddingValues(16.dp)
        ) {
          items(
            count = connections.itemCount,
            key = { index -> connections[index]?.id ?: index }
          ) { index ->
            connections[index]?.let { connection ->
              ConnectionItem(
                connection = connection,
                onEdit = { onEdit(connection) },
                onDelete = { onDelete(connection) },
                onSetDefault = { onSetDefault(connection) }
              )
            }
          }
        }
      }
    }
  }
}

/**
 * Individual connection item with actions.
 */
@Composable
private fun ConnectionItem(
  connection: ConnectionSettings,
  onEdit: () -> Unit,
  onDelete: () -> Unit,
  onSetDefault: () -> Unit
) {
  Card(
    modifier = Modifier
      .fillMaxWidth()
      .clickable { onSetDefault() },
    colors = CardDefaults.cardColors(
      containerColor = if (connection.isDefault) {
        MaterialTheme.colorScheme.primaryContainer
      } else {
        MaterialTheme.colorScheme.surface
      }
    )
  ) {
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .padding(16.dp),
      verticalAlignment = Alignment.CenterVertically
    ) {
      Column(modifier = Modifier.weight(1f)) {
        Text(
          text = connection.name,
          style = MaterialTheme.typography.titleMedium,
          fontWeight = FontWeight.Bold,
          color = if (connection.isDefault) {
            MaterialTheme.colorScheme.onPrimaryContainer
          } else {
            MaterialTheme.colorScheme.onSurface
          },
          maxLines = 1,
          overflow = TextOverflow.Ellipsis
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
          text = "${connection.address}:${connection.port}",
          style = MaterialTheme.typography.bodyMedium,
          color = if (connection.isDefault) {
            MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.9f)
          } else {
            MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
          }
        )
      }

      if (connection.isDefault) {
        Icon(
          imageVector = Icons.Filled.Check,
          contentDescription = "Default connection",
          tint = MaterialTheme.colorScheme.onPrimaryContainer,
          modifier = Modifier
            .padding(horizontal = 8.dp)
            .size(24.dp)
        )
      }

      ConnectionOverflowMenu(
        connection = connection,
        onSetDefault = onSetDefault,
        onEdit = onEdit,
        onDelete = onDelete
      )
    }
  }
}

/**
 * Overflow menu for connection item actions.
 */
@Composable
private fun ConnectionOverflowMenu(
  connection: ConnectionSettings,
  onSetDefault: () -> Unit,
  onEdit: () -> Unit,
  onDelete: () -> Unit
) {
  var showDropdown by remember { mutableStateOf(false) }

  Box {
    IconButton(onClick = { showDropdown = true }) {
      Icon(
        imageVector = Icons.Filled.MoreVert,
        contentDescription = "More options"
      )
    }

    DropdownMenu(
      expanded = showDropdown,
      onDismissRequest = { showDropdown = false }
    ) {
      if (!connection.isDefault) {
        DropdownMenuItem(
          text = { Text(stringResource(R.string.connection_manager_set_default)) },
          onClick = {
            onSetDefault()
            showDropdown = false
          },
          leadingIcon = {
            Icon(Icons.Filled.Check, contentDescription = null)
          }
        )
      }
      DropdownMenuItem(
        text = { Text(stringResource(R.string.common_edit)) },
        onClick = {
          onEdit()
          showDropdown = false
        },
        leadingIcon = {
          Icon(Icons.Filled.Edit, contentDescription = null)
        }
      )
      DropdownMenuItem(
        text = { Text(stringResource(R.string.connection_manager_delete)) },
        onClick = {
          onDelete()
          showDropdown = false
        },
        leadingIcon = {
          Icon(Icons.Filled.Delete, contentDescription = null)
        }
      )
    }
  }
}

/**
 * Empty state when no connections are configured.
 */
@Composable
private fun EmptyConnectionsState() {
  Box(
    modifier = Modifier.fillMaxSize(),
    contentAlignment = Alignment.Center
  ) {
    Column(
      horizontalAlignment = Alignment.CenterHorizontally,
      verticalArrangement = Arrangement.spacedBy(16.dp),
      modifier = Modifier.padding(32.dp)
    ) {
      Icon(
        imageVector = Icons.Filled.Computer,
        contentDescription = null,
        modifier = Modifier.size(64.dp),
        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
      )

      Text(
        text = stringResource(R.string.connection_manager_no_connections),
        style = MaterialTheme.typography.titleLarge,
        fontWeight = FontWeight.Medium,
        color = MaterialTheme.colorScheme.onSurface
      )

      Text(
        text = stringResource(R.string.connection_manager_no_connections_hint),
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        textAlign = TextAlign.Center,
        modifier = Modifier.padding(horizontal = 16.dp)
      )
    }
  }
}

/**
 * Dialog for adding or editing a connection.
 */
@Composable
fun AddEditConnectionDialog(
  connection: ConnectionSettings? = null,
  onDismiss: () -> Unit,
  onSave: (ConnectionSettings) -> Unit
) {
  val portErrorMessage = stringResource(R.string.connection_manager_port_error)
  val state = rememberAddEditConnectionDialogState(connection, portErrorMessage)

  AlertDialog(
    onDismissRequest = onDismiss,
    title = {
      Text(
        text = if (state.isEdit) {
          stringResource(R.string.common_edit)
        } else {
          stringResource(R.string.common_add)
        }
      )
    },
    text = {
      ConnectionFormFields(
        name = state.name,
        onNameChange = state::updateName,
        address = state.address,
        onAddressChange = state::updateAddress,
        port = state.port,
        onPortChange = state::updatePort,
        portError = state.portError
      )
    },
    confirmButton = {
      ConnectionDialogSaveButton(
        isEdit = state.isEdit,
        isValid = state.isValid,
        connection = connection,
        state = state,
        onSave = onSave
      )
    },
    dismissButton = {
      TextButton(onClick = onDismiss) {
        Text(stringResource(android.R.string.cancel))
      }
    }
  )
}

/**
 * Form fields for connection dialog.
 */
@Composable
private fun ConnectionFormFields(
  name: String,
  onNameChange: (String) -> Unit,
  address: String,
  onAddressChange: (String) -> Unit,
  port: String,
  onPortChange: (String) -> Unit,
  portError: String?
) {
  Column {
    OutlinedTextField(
      value = name,
      onValueChange = onNameChange,
      label = { Text(stringResource(R.string.settings_dialog_hint_name)) },
      modifier = Modifier.fillMaxWidth(),
      singleLine = true
    )

    Spacer(modifier = Modifier.height(8.dp))

    OutlinedTextField(
      value = address,
      onValueChange = onAddressChange,
      label = { Text(stringResource(R.string.settings_dialog_hint_host)) },
      modifier = Modifier.fillMaxWidth(),
      singleLine = true
    )

    Spacer(modifier = Modifier.height(8.dp))

    OutlinedTextField(
      value = port,
      onValueChange = onPortChange,
      label = { Text(stringResource(R.string.settings_dialog_hint_port)) },
      modifier = Modifier.fillMaxWidth(),
      keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
      isError = portError != null,
      supportingText = portError?.let { { Text(it) } },
      singleLine = true
    )
  }
}

/**
 * Save button for connection dialog with validation.
 */
@Composable
private fun ConnectionDialogSaveButton(
  isEdit: Boolean,
  isValid: Boolean,
  connection: ConnectionSettings?,
  state: AddEditConnectionDialogState,
  onSave: (ConnectionSettings) -> Unit
) {
  TextButton(
    onClick = {
      if (isValid && state.portNumber in 1..65535) {
        onSave(state.toConnectionSettings(connection))
      }
    },
    enabled = isValid
  ) {
    Text(
      if (isEdit) {
        stringResource(R.string.settings_dialog_save)
      } else {
        stringResource(R.string.common_add)
      }
    )
  }
}

/**
 * Floating Action Button menu with labeled options.
 */
@Composable
internal fun FabMenu(
  isExpanded: Boolean,
  isScanning: Boolean = false,
  onToggle: () -> Unit,
  onAddConnection: () -> Unit,
  onScanNetwork: () -> Unit
) {
  Column(
    horizontalAlignment = Alignment.End
  ) {
    if (isExpanded && !isScanning) {
      Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(bottom = 16.dp)
      ) {
        Text(
          text = stringResource(R.string.connection_manager_scan),
          style = MaterialTheme.typography.bodyMedium,
          modifier = Modifier
            .padding(end = 16.dp)
            .wrapContentSize()
        )
        SmallFloatingActionButton(
          onClick = onScanNetwork,
          containerColor = MaterialTheme.colorScheme.primary,
          contentColor = MaterialTheme.colorScheme.onPrimary
        ) {
          Icon(
            imageVector = Icons.Filled.Search,
            contentDescription = stringResource(R.string.connection_manager_scan)
          )
        }
      }

      Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(bottom = 16.dp)
      ) {
        Text(
          text = stringResource(R.string.common_add),
          style = MaterialTheme.typography.bodyMedium,
          modifier = Modifier
            .padding(end = 16.dp)
            .wrapContentSize()
        )
        SmallFloatingActionButton(
          onClick = onAddConnection,
          containerColor = MaterialTheme.colorScheme.primary,
          contentColor = MaterialTheme.colorScheme.onPrimary
        ) {
          Icon(
            imageVector = Icons.Filled.Add,
            contentDescription = stringResource(R.string.common_add)
          )
        }
      }
    }

    if (!isScanning) {
      FloatingActionButton(
        onClick = onToggle,
        containerColor = MaterialTheme.colorScheme.primary,
        contentColor = MaterialTheme.colorScheme.onPrimary
      ) {
        Icon(
          imageVector = if (isExpanded) Icons.Filled.Check else Icons.Filled.Menu,
          contentDescription = if (isExpanded) {
            "Close menu"
          } else {
            "Open menu"
          }
        )
      }
    }
  }
}

/**
 * Connection Manager screen configuration implementing ConfigurableScreen.
 * Provides FAB and snackbar message configuration for the scaffold.
 */
private class ConnectionManagerScreenConfig(
  private val viewModel: ConnectionManagerViewModel,
  private val state: ConnectionManagerScreenState,
  private val context: Context
) : ConfigurableScreen {

  @Composable
  override fun getScreenConfig(): ScreenConfig {
    val snackbarMessages = remember(viewModel.state.events) {
      viewModel.state.events.map { event ->
        when (event) {
          DiscoveryStop.NoWifi -> context.getString(R.string.connection_manager_discovery_no_wifi)
          DiscoveryStop.NotFound -> context.getString(
            R.string.connection_manager_discovery_not_found
          )
          is DiscoveryStop.Complete -> context.getString(
            R.string.connection_manager_discovery_success
          )
        }
      }
    }

    return ScreenConfig(
      floatingActionButton = {
        FabMenu(
          isExpanded = state.isFabMenuExpanded,
          isScanning = state.isScanning,
          onToggle = state::toggleFabMenu,
          onAddConnection = state::showAddDialog,
          onScanNetwork = state::startScanning
        )
      },
      snackbarMessages = snackbarMessages
    )
  }
}
