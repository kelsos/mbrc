package com.kelsos.mbrc.features.output

import android.app.Dialog
import android.os.Bundle
import android.view.MotionEvent
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.ProgressBar
import android.widget.Spinner
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.core.view.isInvisible
import androidx.core.view.isVisible
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.kelsos.mbrc.R
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import org.koin.android.scope.AndroidScopeComponent
import org.koin.androidx.scope.fragmentScope
import org.koin.androidx.viewmodel.ext.android.viewModel
import org.koin.core.scope.Scope

class OutputSelectionDialog :
  DialogFragment(),
  AndroidScopeComponent,
  View.OnTouchListener {
  override val scope: Scope by fragmentScope()

  private var touchInitiated: Boolean = false
  private lateinit var fm: FragmentManager
  private lateinit var dialog: AlertDialog

  private lateinit var availableOutputs: Spinner
  private lateinit var loadingProgress: ProgressBar
  private lateinit var errorMessage: TextView

  private val viewModel: OutputSelectionViewModel by viewModel()

  private val onItemSelectedListener: AdapterView.OnItemSelectedListener by lazy {
    val vm = viewModel

    object : AdapterView.OnItemSelectedListener {
      override fun onNothingSelected(parent: AdapterView<*>?) {
        // We don't handle this
      }

      override fun onItemSelected(
        parent: AdapterView<*>?,
        view: View?,
        position: Int,
        id: Long,
      ) {
        if (!touchInitiated) {
          return
        }

        val selectedOutput = availableOutputs.adapter.getItem(position) as String
        vm.setOutput(selectedOutput)
        touchInitiated = false
      }
    }
  }

  override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
    val context = requireContext()
    val view = layoutInflater.inflate(R.layout.dialog_output_selection, null, false)

    availableOutputs = view.findViewById(R.id.output_selection__available_outputs)
    loadingProgress = view.findViewById(R.id.output_selection__loading_outputs)
    errorMessage = view.findViewById(R.id.output_selection__error_message)

    dialog =
      MaterialAlertDialogBuilder(context)
        .setTitle(R.string.output_selection__select_output)
        .setView(view)
        .setNeutralButton(R.string.output_selection__close_dialog) { dialogInterface, _ ->
          dialogInterface.dismiss()
        }.create()

    lifecycleScope.launch {
      repeatOnLifecycle(Lifecycle.State.STARTED) {
        viewModel.outputs.collectLatest {
          update(it.devices)

          val adapter = availableOutputs.adapter as ArrayAdapter<*>
          for (position in 0 until adapter.count) {
            if (it.active == adapter.getItem(position)) {
              availableOutputs.setSelection(position, false)
              break
            }
          }
        }

        viewModel.events.collectLatest {
          when (it) {
            OutputSelectionResult.Success -> {
              availableOutputs.isVisible = true
              errorMessage.isInvisible = true
            }

            else -> error(it)
          }
        }
      }
    }

    viewModel.reload()

    return dialog
  }

  override fun onTouch(
    view: View?,
    event: MotionEvent?,
  ): Boolean {
    touchInitiated = true
    return view?.performClick() == true
  }

  private fun update(data: List<String>) {
    availableOutputs.onItemSelectedListener = null
    availableOutputs.setOnTouchListener(null)
    val outputAdapter =
      ArrayAdapter(
        requireContext(),
        R.layout.item_output_device,
        R.id.output_selection__output_device,
        data,
      )
    availableOutputs.adapter = outputAdapter
    availableOutputs.onItemSelectedListener = onItemSelectedListener
    availableOutputs.setOnTouchListener(this)
    loadingProgress.isVisible = false
    availableOutputs.isVisible = true
  }

  fun error(result: OutputSelectionResult) {
    val resId =
      when (result) {
        OutputSelectionResult.ConnectionError -> R.string.output_selection__connection_error
        else -> R.string.output_selection__generic_error
      }
    errorMessage.setText(resId)
    loadingProgress.isVisible = false
    availableOutputs.isInvisible = true
    errorMessage.isVisible = true
  }

  override fun dismiss() {
    dialog.dismiss()
  }

  fun show() {
    show(fm, TAG)
  }

  companion object {
    private const val TAG = "output_selection_dialog"

    fun create(fm: FragmentManager): OutputSelectionDialog {
      val dialog = OutputSelectionDialog()
      dialog.fm = fm
      return dialog
    }
  }
}
