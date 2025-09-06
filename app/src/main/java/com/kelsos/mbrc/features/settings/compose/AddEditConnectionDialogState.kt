package com.kelsos.mbrc.features.settings.compose

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.kelsos.mbrc.features.settings.ConnectionSettings

/**
 * State holder for AddEditConnectionDialog that manages form state and validation.
 */
@Stable
class AddEditConnectionDialogState(
  initialConnection: ConnectionSettings?,
  private val portErrorMessage: String
) {
  var name by mutableStateOf(initialConnection?.name.orEmpty())
    private set

  var address by mutableStateOf(initialConnection?.address.orEmpty())
    private set

  var port by mutableStateOf(initialConnection?.port?.toString() ?: "3000")
    private set

  var portError by mutableStateOf<String?>(null)
    private set

  val isEdit: Boolean = initialConnection != null

  val isValid: Boolean
    get() = name.isNotEmpty() && address.isNotEmpty() && portError == null

  val portNumber: Int
    get() = port.toIntOrNull() ?: 0

  /**
   * Updates the connection name.
   */
  fun updateName(newName: String) {
    name = newName
  }

  /**
   * Updates the connection address.
   */
  fun updateAddress(newAddress: String) {
    address = newAddress
  }

  /**
   * Updates the port and validates it.
   */
  fun updatePort(newPort: String) {
    port = newPort
    validatePort(newPort)
  }

  /**
   * Validates port input and sets error state.
   */
  private fun validatePort(portValue: String) {
    val portNum = portValue.toIntOrNull() ?: 0
    portError = if (portNum !in 1..65535) {
      portErrorMessage
    } else {
      null
    }
  }

  /**
   * Creates a ConnectionSettings from the current form state.
   */
  fun toConnectionSettings(baseConnection: ConnectionSettings?): ConnectionSettings =
    (baseConnection ?: ConnectionSettings.default()).copy(
      name = name,
      address = address,
      port = portNumber
    )
}

/**
 * Remember function for AddEditConnectionDialogState.
 */
@Composable
fun rememberAddEditConnectionDialogState(
  connection: ConnectionSettings?,
  portErrorMessage: String
): AddEditConnectionDialogState = remember(connection) {
  AddEditConnectionDialogState(connection, portErrorMessage)
}
