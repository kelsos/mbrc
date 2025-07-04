package com.kelsos.mbrc.features.minicontrol

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import androidx.core.app.TaskStackBuilder
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import coil3.load
import coil3.request.crossfade
import coil3.request.error
import coil3.request.placeholder
import coil3.size.Scale
import com.google.android.material.progressindicator.LinearProgressIndicator
import com.kelsos.mbrc.R
import com.kelsos.mbrc.common.state.PlayerState
import com.kelsos.mbrc.extensions.getDimens
import com.kelsos.mbrc.features.player.PlayerActivity
import kotlinx.coroutines.launch
import org.koin.androidx.scope.ScopeFragment
import org.koin.androidx.viewmodel.ext.android.viewModel

class MiniControlFragment : ScopeFragment() {
  private lateinit var trackCover: ImageView
  private lateinit var trackArtist: TextView
  private lateinit var trackTitle: TextView
  private lateinit var playPause: ImageButton
  private lateinit var progressIndicator: LinearProgressIndicator

  private val viewModel: MiniControlViewModel by viewModel()

  override fun onCreateView(
    inflater: LayoutInflater,
    container: ViewGroup?,
    savedInstanceState: Bundle?,
  ): View? {
    val view = inflater.inflate(R.layout.fragment_mini_control, container, false)

    trackArtist = view.findViewById(R.id.mc_track_artist)
    trackTitle = view.findViewById(R.id.mc_track_title)
    trackCover = view.findViewById(R.id.mc_track_cover)
    progressIndicator = view.findViewById(R.id.mini_control_track_progress)
    playPause = view.findViewById(R.id.mc_play_pause)
    playPause.setOnClickListener { viewModel.perform(MiniControlAction.PlayPause) }
    view.findViewById<ImageButton>(R.id.mc_next_track).setOnClickListener {
      viewModel.perform(MiniControlAction.PlayNext)
    }
    view.findViewById<ImageButton>(R.id.mc_prev_track).setOnClickListener {
      viewModel.perform(MiniControlAction.PlayPrevious)
    }
    view.findViewById<View>(R.id.mini_control).setOnClickListener {
      val builder = TaskStackBuilder.create(requireContext())
      builder.addNextIntentWithParentStack(Intent(context, PlayerActivity::class.java))
      builder.startActivities()
    }

    lifecycleScope.launch {
      viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
        viewModel.state.collect { state ->
          trackArtist.text = state.playingTrack.artist
          trackTitle.text = state.playingTrack.title

          when (state.playingState) {
            PlayerState.Playing -> playPause.setImageResource(R.drawable.baseline_pause_24)
            else -> playPause.setImageResource(R.drawable.baseline_play_arrow_24)
          }

          updateCover(state.playingTrack.coverUrl)

          progressIndicator.progress = state.playingPosition.current.toInt()
          progressIndicator.max = state.playingPosition.total.toInt()
        }
      }
    }

    return view
  }

  private fun updateCover(path: String) {
    val dimens = requireContext().getDimens()
    trackCover.load(path) {
      crossfade(false)
      placeholder(R.drawable.ic_image_no_cover)
      error(R.drawable.ic_image_no_cover)
      size(dimens)
      scale(Scale.FILL)
    }
  }
}
