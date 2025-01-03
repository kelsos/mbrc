package com.kelsos.mbrc.features.player

import android.app.Dialog
import android.os.Bundle
import android.widget.RatingBar
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.kelsos.mbrc.R
import kotlinx.coroutines.launch
import org.koin.android.scope.AndroidScopeComponent
import org.koin.androidx.scope.fragmentScope
import org.koin.androidx.viewmodel.ext.android.viewModel

class RatingDialogFragment :
  DialogFragment(),
  AndroidScopeComponent {
  private val viewModel: RatingDialogViewModel by viewModel()
  private var ratingBar: RatingBar? = null

  override val scope by fragmentScope()

  override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
    val dialog =
      MaterialAlertDialogBuilder(requireContext())
        .setTitle(R.string.rate_the_playing_track)
        .setView(R.layout.ui_dialog_rating)
        .show()

    ratingBar = dialog.findViewById(R.id.ratingBar)
    ratingBar?.setOnRatingBarChangeListener { _, ratingValue, isUserInitiated ->
      if (isUserInitiated) {
        viewModel.changeRating(ratingValue)
      }
    }

    lifecycleScope.launch {
      repeatOnLifecycle(Lifecycle.State.STARTED) {
        viewModel.rating.collect {
          ratingBar?.rating = it
        }
      }
    }
    return dialog
  }
}
