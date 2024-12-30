package com.kelsos.mbrc.output

import android.annotation.SuppressLint
import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
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
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.kelsos.mbrc.R
import com.kelsos.mbrc.di.modules.obtainViewModel
import toothpick.Scope
import toothpick.Toothpick

class OutputSelectionDialog() : DialogFragment(), View.OnTouchListener {

  private var touchInitiated: Boolean = false
  private lateinit var fm: FragmentManager
  private lateinit var dialog: AlertDialog

  private lateinit var availableOutputs: Spinner
  private lateinit var loadingProgress: ProgressBar
  private lateinit var errorMessage: TextView

  private var scope: Scope? = null

  private val onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
    override fun onNothingSelected(parent: AdapterView<*>?) {
    }

    override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
      if (!touchInitiated) {
        return
      }

      val selectedOutput = availableOutputs.adapter.getItem(position) as String
      viewModel.setOutput(selectedOutput)
      touchInitiated = false
    }
  }

  private lateinit var viewModel: OutputSelectionViewModel

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    scope = Toothpick.openScopes(requireActivity().application, this)
    Toothpick.inject(this, scope)
  }

  override fun onActivityCreated(savedInstanceState: Bundle?) {
    super.onActivityCreated(savedInstanceState)
    viewModel.outputs.observe(this) {
      update(it)
    }
    viewModel.selection.observe(this) {
      val adapter = availableOutputs.adapter as ArrayAdapter<*>
      for (position in 0 until adapter.count) {
        if (it == adapter.getItem(position)) {
          availableOutputs.setSelection(position, false)
          break
        }
      }
    }
    viewModel.events.observe(this) {
      if (it.handled) {
        return@observe
      }
      it.handled = true
      when (it) {
        OutputSelectionResult.Success -> {
          availableOutputs.isVisible = true
          errorMessage.isInvisible = true
        }
        else -> error(it)
      }
    }
    viewModel.reload()
  }

  @SuppressLint("InflateParams")
  override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
    val context = requireContext()
    val inflater = LayoutInflater.from(context)
    val view = inflater.inflate(R.layout.dialog__output_selection, null, false)

    availableOutputs = view.findViewById(R.id.output_selection__available_outputs)
    loadingProgress = view.findViewById(R.id.output_selection__loading_outputs)
    errorMessage = view.findViewById(R.id.output_selection__error_message)

    viewModel = obtainViewModel(OutputSelectionViewModel::class.java)

    dialog = MaterialAlertDialogBuilder(context)
      .setTitle(R.string.output_selection__select_output)
      .setView(view)
      .setNeutralButton(R.string.output_selection__close_dialog) { dialogInterface, _ ->
        dialogInterface.dismiss()
      }
      .create()

    return dialog
  }

  override fun onDestroy() {
    Toothpick.closeScope(this)
    super.onDestroy()
  }

  override fun onTouch(view: View?, event: MotionEvent?): Boolean {
    touchInitiated = true
    return view?.performClick() == true
  }

  private fun update(data: List<String>) {
    availableOutputs.onItemSelectedListener = null
    availableOutputs.setOnTouchListener(null)
    val outputAdapter = ArrayAdapter(
      requireContext(),
      R.layout.item__output_device,
      R.id.output_selection__output_device,
      data
    )
    availableOutputs.adapter = outputAdapter
    availableOutputs.onItemSelectedListener = onItemSelectedListener
    availableOutputs.setOnTouchListener(this)
    loadingProgress.isVisible = false
    availableOutputs.isVisible = true
  }

  fun error(result: OutputSelectionResult) {
    val resId = when (result) {
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
