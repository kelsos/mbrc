package com.kelsos.mbrc.features.settings

import android.app.Dialog
import android.content.Context
import android.os.Bundle
import android.text.TextUtils
import android.widget.EditText
import androidx.fragment.app.DialogFragment
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.kelsos.mbrc.R

class SettingsDialogFragment : DialogFragment() {
  lateinit var hostEdit: EditText
  lateinit var nameEdit: EditText
  lateinit var portEdit: EditText

  private var saveListener: SettingsSaveListener? = null
  private lateinit var settings: ConnectionSettings
  private var edit: Boolean = false

  private fun setConnectionSettings(settings: ConnectionSettings) {
    this.settings = settings
  }

  override fun onAttach(context: Context) {
    super.onAttach(context)
    require(context is SettingsSaveListener) {
      "Activity must implement fragment's listener."
    }
    saveListener = context
  }

  override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
    val dialog =
      MaterialAlertDialogBuilder(requireActivity())
        .setView(R.layout.dialog_settings)
        .setTitle(if (edit) R.string.common_edit else R.string.common_add)
        .setPositiveButton(if (edit) R.string.settings_dialog_save else R.string.common_add) {
            dialog,
            _
          ->
          var shouldIClose = true
          val hostname = hostEdit.text.toString()
          val computerName = nameEdit.text.toString()

          if (hostname.isEmpty() || computerName.isEmpty()) {
            shouldIClose = false
          }

          val portText = portEdit.text.toString()

          val portNum = if (TextUtils.isEmpty(portText)) 0 else Integer.parseInt(portText)
          if (isValid(portNum) && shouldIClose) {
            saveListener?.onSave(
              settings.copy(
                name = computerName,
                address = hostname,
                port = portNum
              )
            )
            dialog.dismiss()
          }
        }.setNegativeButton(android.R.string.cancel) { dialog, _ -> dialog.dismiss() }
        .show()

    hostEdit = requireNotNull(dialog.findViewById(R.id.settings_dialog_host))
    nameEdit = requireNotNull(dialog.findViewById(R.id.settings_dialog_name))
    portEdit = requireNotNull(dialog.findViewById(R.id.settings_dialog_port))
    return dialog
  }

  override fun onStart() {
    super.onStart()
    nameEdit.setText(settings.name)
    hostEdit.setText(settings.address)

    if (settings.port > 0) {
      portEdit.setText(getString(R.string.common_number, settings.port))
    }
  }

  private fun isValid(port: Int): Boolean = if (port < MIN_PORT || port > MAX_PORT) {
    portEdit.error = getString(R.string.alert_invalid_port_number)
    false
  } else {
    true
  }

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    if (!edit) {
      settings = ConnectionSettings.default()
    }
  }

  fun interface SettingsSaveListener {
    fun onSave(settings: ConnectionSettings)
  }

  companion object {
    private const val MAX_PORT = 65535
    private const val MIN_PORT = 1

    fun newInstance(settings: ConnectionSettings): SettingsDialogFragment {
      val fragment = SettingsDialogFragment()
      fragment.setConnectionSettings(settings)
      fragment.edit = true
      return fragment
    }
  }
}
