package com.kelsos.mbrc.features.player

import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.Color
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.Window
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.SeekBar
import android.widget.TextView
import androidx.activity.OnBackPressedCallback
import androidx.annotation.DrawableRes
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.widget.ShareActionProvider
import androidx.core.app.ShareCompat
import androidx.core.content.ContextCompat
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.core.view.MenuItemCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import coil3.load
import coil3.request.crossfade
import coil3.request.error
import coil3.request.placeholder
import coil3.size.Scale
import com.google.android.material.color.MaterialColors
import com.google.android.material.transition.platform.MaterialContainerTransformSharedElementCallback
import com.kelsos.mbrc.BaseActivity
import com.kelsos.mbrc.R
import com.kelsos.mbrc.changelog.ChangelogDialog
import com.kelsos.mbrc.common.state.LfmRating
import com.kelsos.mbrc.common.state.PlayerState
import com.kelsos.mbrc.common.state.PlayerStatusModel
import com.kelsos.mbrc.common.state.PlayingPosition
import com.kelsos.mbrc.common.state.PlayingTrack
import com.kelsos.mbrc.common.state.Repeat
import com.kelsos.mbrc.common.state.ShuffleMode
import com.kelsos.mbrc.common.state.TrackRating
import com.kelsos.mbrc.extensions.getDimens
import kotlinx.coroutines.launch
import org.koin.androidx.viewmodel.ext.android.viewModel

class PlayerActivity : BaseActivity(R.layout.activity_main) {
  private val viewModel: PlayerViewModel by viewModel()

  private lateinit var artistLabel: TextView
  private lateinit var titleLabel: TextView
  private lateinit var albumLabel: TextView
  private lateinit var trackProgressCurrent: TextView
  private lateinit var trackDuration: TextView
  private lateinit var playPauseButton: ImageButton
  private lateinit var volumeBar: SeekBar
  private lateinit var progressBar: SeekBar
  private lateinit var muteButton: ImageButton
  private lateinit var shuffleButton: ImageButton
  private lateinit var repeatButton: ImageButton
  private lateinit var albumCover: ImageView

  override fun active(): Int = R.id.nav_home

  private var shareActionProvider: ShareActionProvider? = null

  private var changeLogDialog: AlertDialog? = null

  private var menu: Menu? = null

  private val activeColor by lazy { ContextCompat.getColor(this, R.color.md_color_secondary) }
  private val inactiveColor by lazy {
    return@lazy MaterialColors.getColor(
      this@PlayerActivity,
      android.R.attr.colorControlNormal,
      Color.GRAY
    )
  }

  override fun onCreate(savedInstanceState: Bundle?) {
    installSplashScreen()
    window.requestFeature(Window.FEATURE_ACTIVITY_TRANSITIONS)
    setExitSharedElementCallback(MaterialContainerTransformSharedElementCallback())
    window.sharedElementsUseOverlay = false
    super.onCreate(savedInstanceState)

    onBackPressedDispatcher.addCallback(
      this,
      object : OnBackPressedCallback(true) {
        override fun handleOnBackPressed() {
          finishAfterTransition()
        }
      }
    )

    initViews()
    initListeners()
    observeViewModel()
  }

  private fun observeViewModel() {
    lifecycleScope.launch {
      repeatOnLifecycle(Lifecycle.State.STARTED) {
        viewModel.events.collect { event ->
          processEvent(event)
        }
      }
    }

    lifecycleScope.launch {
      repeatOnLifecycle(Lifecycle.State.STARTED) {
        viewModel.state.collect { playerState ->
          updatePlayerTrack(playerState.playingTrack)
          updatePlayerStatus(playerState.playerStatus)
          updateTrackRating(playerState.trackRating)
          updatePlayingPosition(playerState.playingPosition)
        }
      }
    }
  }

  private fun processEvent(event: PlayerUiMessage) {
    when (event) {
      PlayerUiMessage.ShowChangelog -> {
        changeLogDialog = ChangelogDialog.show(this@PlayerActivity, R.raw.changelog)
      }
    }
  }

  private fun initListeners() {
    muteButton.setOnClickListener { viewModel.interact(PlayerAction.ToggleMute) }
    shuffleButton.setOnClickListener { viewModel.interact(PlayerAction.ToggleShuffle) }
    repeatButton.setOnClickListener { viewModel.interact(PlayerAction.ToggleRepeat) }

    playPauseButton.setOnClickListener { viewModel.interact(PlayerAction.ResumePlayOrPause) }
    playPauseButton.setOnLongClickListener {
      viewModel.interact(PlayerAction.Stop)
      true
    }
    findViewById<ImageButton>(R.id.main_button_previous).setOnClickListener {
      viewModel.interact(PlayerAction.PlayPrevious)
    }
    findViewById<ImageButton>(R.id.main_button_next).setOnClickListener {
      viewModel.interact(PlayerAction.PlayNext)
    }
    findViewById<View>(R.id.track_info_area).setOnClickListener { navigate(R.id.nav_now_playing) }
  }

  private fun initViews() {
    artistLabel = findViewById(R.id.main_artist_label)
    titleLabel = findViewById(R.id.main_title_label)
    albumLabel = findViewById(R.id.main_label_album)
    trackProgressCurrent = findViewById(R.id.main_track_progress_current)
    trackDuration = findViewById(R.id.main_track_duration_total)
    playPauseButton = findViewById(R.id.main_button_play_pause)
    volumeBar = findViewById(R.id.main_volume_seeker)
    progressBar = findViewById(R.id.main_track_progress_seeker)
    muteButton = findViewById(R.id.main_mute_button)
    shuffleButton = findViewById(R.id.main_shuffle_button)
    repeatButton = findViewById(R.id.main_repeat_button)
    albumCover = findViewById(R.id.main_album_cover_image_view)
  }

  override fun onNewIntent(intent: Intent) {
    super.onNewIntent(intent)
    if (intent.getBooleanExtra(EXIT_APP, false) == true) {
      exitApplication()
      return
    }
  }

  override fun onStart() {
    super.onStart()
    volumeBar.listen { viewModel.interact(PlayerAction.ChangeVolume(it)) }
    progressBar.listen { viewModel.interact(PlayerAction.Seek(it)) }
    artistLabel.isSelected = true
    titleLabel.isSelected = true
    albumLabel.isSelected = true
  }

  override fun onOptionsItemSelected(item: MenuItem): Boolean = when (item.itemId) {
    R.id.menu_lastfm_scrobble -> {
      viewModel.interact(PlayerAction.ToggleScrobbling)
      true
    }

    R.id.menu_rating_dialog -> {
      val ratingDialog = RatingDialogFragment()
      ratingDialog.show(supportFragmentManager, "RatingDialog")
      true
    }

    R.id.menu_lastfm_love -> {
      viewModel.interact(PlayerAction.ToggleFavorite)
      true
    }

    else -> false
  }

  override fun onStop() {
    super.onStop()
    volumeBar.removeListener()
    progressBar.removeListener()
  }

  override fun onCreateOptionsMenu(menu: Menu): Boolean {
    menuInflater.inflate(R.menu.menu, menu)
    this.menu = menu

    val shareItem = menu.findItem(R.id.actionbar_share)
    val shareActionProvider = ShareActionProvider(this)
    MenuItemCompat.setActionProvider(shareItem, shareActionProvider)
    shareItem.setOnMenuItemClickListener {
      startActivity(Intent.createChooser(shareIntent, "Share via"))
      true
    }
    this.shareActionProvider = shareActionProvider
    return super.onCreateOptionsMenu(menu)
  }

  private val shareIntent: Intent
    get() =
      ShareCompat
        .IntentBuilder(this)
        .setType("text/plain")
        .setChooserTitle("Share via")
        .setText("Now Playing: ${artistLabel.text} - ${titleLabel.text}")
        .intent

  fun updatePlayerTrack(track: PlayingTrack) {
    artistLabel.text = track.artist
    titleLabel.text = track.title
    albumLabel.text =
      if (track.year.isEmpty()) {
        track.album
      } else {
        "${track.album} [${track.year}]"
      }

    shareActionProvider?.setShareIntent(shareIntent)

    val dimens = getDimens()
    albumCover.load(track.coverUrl) {
      crossfade(false)
      placeholder(R.drawable.ic_image_no_cover)
      error(R.drawable.ic_image_no_cover)
      size(dimens)
      scale(Scale.FILL)
    }
  }

  private fun updatePlayerStatus(status: PlayerStatusModel) {
    updateShuffleState(status.shuffle)
    updateRepeat(status.repeat)
    updateVolume(status.volume, status.mute)
    updatePlayState(status.state)
    updateScrobbleStatus(status.scrobbling)
  }

  private fun updateShuffleState(shuffleModel: ShuffleMode) {
    val shuffle = ShuffleMode.Off != shuffleModel
    val autoDj = ShuffleMode.AutoDJ == shuffleModel
    shuffleButton.imageTintList =
      ColorStateList.valueOf(if (shuffle) activeColor else inactiveColor)
    shuffleButton.setImageResource(
      if (autoDj) R.drawable.baseline_headset_24 else R.drawable.baseline_shuffle_24
    )
  }

  private fun updateRepeat(mode: Repeat) {
    var color = activeColor

    @DrawableRes var resId = R.drawable.baseline_repeat_24

    when (mode) {
      Repeat.One -> resId = R.drawable.baseline_repeat_one_24
      else -> color = inactiveColor
    }

    repeatButton.setImageResource(resId)
    repeatButton.imageTintList = ColorStateList.valueOf(color)
  }

  private fun updateVolume(volume: Int, mute: Boolean) {
    volumeBar.progress = if (mute) 0 else volume
    muteButton.imageTintList = ColorStateList.valueOf(inactiveColor)
    muteButton.setImageResource(
      if (mute) R.drawable.baseline_volume_off_24 else R.drawable.baseline_volume_up_24
    )
  }

  private fun updatePlayState(state: PlayerState) {
    val accentColor = activeColor
    val tag = state.state

    if (playPauseButton.tag == tag) {
      return
    }
    @DrawableRes val resId: Int =
      when (state) {
        PlayerState.Playing -> R.drawable.baseline_pause_circle_filled_24
        else -> R.drawable.baseline_play_circle_filled_24
      }

    playPauseButton.imageTintList = ColorStateList.valueOf(accentColor)
    playPauseButton.setImageResource(resId)
    playPauseButton.tag = tag
  }

  private fun updateTrackRating(rating: TrackRating) {
    updateLfmRating(rating.lfmRating)
  }

  private fun updatePlayingPosition(position: PlayingPosition) {
    if (progressBar.max == position.total.toInt() &&
      progressBar.progress == position.current.toInt()
    ) {
      return
    }
    progressBar.max = position.total.toInt()
    progressBar.progress = position.current.toInt()
    trackDuration.text = position.totalMinutes
    trackProgressCurrent.text = position.currentMinutes
  }

  fun updateScrobbleStatus(active: Boolean) {
    menu?.findItem(R.id.menu_lastfm_scrobble)?.isChecked = active
  }

  fun updateLfmRating(rating: LfmRating) {
    val menu = menu ?: return
    val favoriteMenuItem = menu.findItem(R.id.menu_lastfm_love) ?: return

    when (rating) {
      LfmRating.Loved -> favoriteMenuItem.setIcon(R.drawable.baseline_favorite_24)
      else -> favoriteMenuItem.setIcon(R.drawable.baseline_favorite_border_24)
    }
  }

  override fun onDestroy() {
    changeLogDialog?.dismiss()
    super.onDestroy()
  }
}
