package com.kelsos.mbrc.features.minicontrol

import android.content.Intent
import android.graphics.Bitmap
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import androidx.core.app.TaskStackBuilder
import androidx.fragment.app.Fragment
import com.kelsos.mbrc.R
import com.kelsos.mbrc.annotations.PlayerState
import com.kelsos.mbrc.annotations.PlayerState.State
import com.kelsos.mbrc.extensions.getDimens
import com.kelsos.mbrc.features.player.PlayerActivity
import com.kelsos.mbrc.features.player.TrackInfo
import com.squareup.picasso.Picasso
import org.koin.android.ext.android.inject
import org.koin.android.scope.AndroidScopeComponent
import org.koin.androidx.scope.fragmentScope
import org.koin.core.scope.Scope
import java.io.File

class MiniControlFragment :
  Fragment(),
  MiniControlView,
  AndroidScopeComponent {
  private lateinit var trackCover: ImageView
  private lateinit var trackArtist: TextView
  private lateinit var trackTitle: TextView
  private lateinit var playPause: ImageButton

  override val scope: Scope by fragmentScope()

  private val presenter: MiniControlPresenter by inject()

  override fun onCreateView(
    inflater: LayoutInflater,
    container: ViewGroup?,
    savedInstanceState: Bundle?,
  ): View? {
    val view = inflater.inflate(R.layout.ui_fragment_mini_control, container, false)
    checkNotNull(scope)
    trackArtist = view.findViewById(R.id.mc_track_artist)
    trackTitle = view.findViewById(R.id.mc_track_title)
    trackCover = view.findViewById(R.id.mc_track_cover)
    playPause = view.findViewById(R.id.mc_play_pause)
    playPause.setOnClickListener { presenter.playPause() }

    view.findViewById<ImageButton>(R.id.mc_next_track).setOnClickListener { presenter.next() }
    view.findViewById<ImageButton>(R.id.mc_prev_track).setOnClickListener { presenter.previous() }
    view.findViewById<View>(R.id.mini_control).setOnClickListener {
      val builder = TaskStackBuilder.create(requireContext())
      builder.addNextIntentWithParentStack(Intent(context, PlayerActivity::class.java))
      builder.startActivities()
    }

    return view
  }

  override fun onStart() {
    super.onStart()
    presenter.attach(this)
    presenter.load()
  }

  override fun onStop() {
    super.onStop()
    presenter.detach()
  }

  override fun updateCover(path: String) {
    if (activity == null) {
      return
    }

    val file = File(path)

    if (file.exists()) {
      val dimens = requireContext().getDimens()
      Picasso
        .get()
        .load(file)
        .noFade()
        .config(Bitmap.Config.RGB_565)
        .resize(dimens, dimens)
        .centerCrop()
        .into(trackCover)
    } else {
      trackCover.setImageResource(R.drawable.ic_image_no_cover)
    }
  }

  override fun updateTrackInfo(trackInfo: TrackInfo) {
    trackArtist.text = trackInfo.artist
    trackTitle.text = trackInfo.title
  }

  override fun updateState(
    @State state: String,
  ) {
    when (state) {
      PlayerState.PLAYING -> playPause.setImageResource(R.drawable.ic_action_pause)
      else -> playPause.setImageResource(R.drawable.ic_action_play)
    }
  }
}
