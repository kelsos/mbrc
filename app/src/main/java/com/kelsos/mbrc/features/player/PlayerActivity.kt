package com.kelsos.mbrc.features.player

import android.content.Intent
import android.graphics.Bitmap
import android.os.Bundle
import android.text.TextUtils
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.Window
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.SeekBar
import android.widget.TextView
import androidx.activity.OnBackPressedCallback
import androidx.annotation.ColorRes
import androidx.annotation.DrawableRes
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.widget.ShareActionProvider
import androidx.core.content.ContextCompat
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.core.view.MenuItemCompat
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.transition.platform.MaterialContainerTransformSharedElementCallback
import com.kelsos.mbrc.BaseActivity
import com.kelsos.mbrc.R
import com.kelsos.mbrc.UpdateRequiredActivity
import com.kelsos.mbrc.annotations.Connection
import com.kelsos.mbrc.annotations.PlayerState
import com.kelsos.mbrc.annotations.PlayerState.State
import com.kelsos.mbrc.annotations.Repeat
import com.kelsos.mbrc.annotations.Repeat.Mode
import com.kelsos.mbrc.changelog.ChangelogDialog
import com.kelsos.mbrc.events.ui.ShuffleChange
import com.kelsos.mbrc.events.ui.ShuffleChange.ShuffleState
import com.kelsos.mbrc.events.ui.UpdateDuration
import com.kelsos.mbrc.extensions.getDimens
import com.kelsos.mbrc.features.player.ProgressSeekerHelper.ProgressUpdate
import com.squareup.picasso.Picasso
import org.koin.android.ext.android.inject
import java.io.File

class PlayerActivity :
  BaseActivity(),
  PlayerView,
  ProgressUpdate {
  private val presenter: PlayerViewPresenter by inject()
  private val progressHelper: ProgressSeekerHelper by inject()

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

  private var mShareActionProvider: ShareActionProvider? = null

  private var changeLogDialog: AlertDialog? = null
  private var outOfDateDialog: AlertDialog? = null

  private var menu: Menu? = null
  private var volumeChangeListener: SeekBarThrottler? = null
  private var positionChangeListener: SeekBarThrottler? = null

  override fun onCreate(savedInstanceState: Bundle?) {
    installSplashScreen()
    window.requestFeature(Window.FEATURE_ACTIVITY_TRANSITIONS)
    setExitSharedElementCallback(MaterialContainerTransformSharedElementCallback())
    window.sharedElementsUseOverlay = false
    super.onCreate(savedInstanceState)
    setContentView(R.layout.activity_main)

    onBackPressedDispatcher.addCallback(
      this,
      object : OnBackPressedCallback(true) {
        override fun handleOnBackPressed() {
          finishAfterTransition()
        }
      },
    )

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

    muteButton.setOnClickListener { presenter.mute() }
    shuffleButton.setOnClickListener { presenter.shuffle() }
    repeatButton.setOnClickListener { presenter.repeat() }

    playPauseButton.setOnClickListener { presenter.play() }
    playPauseButton.setOnLongClickListener { presenter.stop() }
    findViewById<ImageButton>(R.id.main_button_previous).setOnClickListener { presenter.previous() }
    findViewById<ImageButton>(R.id.main_button_next).setOnClickListener { presenter.next() }
    findViewById<View>(R.id.track_info_area).setOnClickListener { navigate(R.id.nav_now_playing) }

    super.setup()
    presenter.attach(this)
  }

  override fun onNewIntent(intent: Intent) {
    super.onNewIntent(intent)
    if (intent.getBooleanExtra(EXIT_APP, false) == true) {
      exitApplication()
      return
    }
  }

  override fun showChangeLog() {
    changeLogDialog = ChangelogDialog.show(this, R.raw.changelog)
  }

  override fun notifyPluginOutOfDate() {
    val snackBar =
      Snackbar.make(
        navigationView,
        R.string.main__dialog_plugin_outdated_message,
        Snackbar.LENGTH_INDEFINITE,
      )
    snackBar.setAction(android.R.string.ok) { snackBar.dismiss() }
    snackBar.show()
  }

  override fun showPluginUpdateRequired(minimumRequired: String) {
    val intent = Intent(this, UpdateRequiredActivity::class.java)
    intent.putExtra(UpdateRequiredActivity.VERSION, minimumRequired)
    startActivity(intent)
  }

  override fun onStart() {
    super.onStart()

    if (!presenter.isAttached) {
      presenter.attach(this)
    }

    progressHelper.setProgressListener(this)
    volumeChangeListener = SeekBarThrottler { presenter.changeVolume(it) }
    positionChangeListener = SeekBarThrottler { presenter.seek(it) }
    volumeBar.setOnSeekBarChangeListener(volumeChangeListener)
    progressBar.setOnSeekBarChangeListener(positionChangeListener)
    artistLabel.isSelected = true
    titleLabel.isSelected = true
    albumLabel.isSelected = true

    if (!isConnected) {
      onConnectClick()
    }
  }

  override fun onResume() {
    super.onResume()
    presenter.requestNowPlayingPosition()
  }

  override fun onOptionsItemSelected(item: MenuItem): Boolean =
    when (item.itemId) {
      R.id.menu_lastfm_scrobble -> {
        presenter.toggleScrobbling()
        true
      }
      R.id.menu_rating_dialog -> {
        val ratingDialog = RatingDialogFragment()
        ratingDialog.show(supportFragmentManager, "RatingDialog")
        true
      }
      R.id.menu_lastfm_love -> {
        presenter.lfmLove()
      }
      else -> false
    }

  override fun onStop() {
    super.onStop()
    presenter.detach()
    progressHelper.setProgressListener(null)
    volumeChangeListener?.terminate()
    volumeChangeListener = null
    positionChangeListener?.terminate()
    positionChangeListener = null
    volumeBar.setOnSeekBarChangeListener(null)
    progressBar.setOnSeekBarChangeListener(null)
  }

  override fun onCreateOptionsMenu(menu: Menu): Boolean {
    menuInflater.inflate(R.menu.menu, menu)
    this.menu = menu
    val shareItem = menu.findItem(R.id.actionbar_share)
    mShareActionProvider = MenuItemCompat.getActionProvider(shareItem) as ShareActionProvider
    mShareActionProvider!!.setShareIntent(shareIntent)
    return super.onCreateOptionsMenu(menu)
  }

  private val shareIntent: Intent
    get() {
      val shareIntent = Intent(Intent.ACTION_SEND)
      shareIntent.type = "text/plain"
      val payload = String.format("Now Playing: %s - %s", artistLabel.text, titleLabel.text)
      shareIntent.putExtra(Intent.EXTRA_TEXT, payload)
      return shareIntent
    }

  override fun updateCover(path: String) {
    val file = File(path)

    if (!file.exists()) {
      albumCover.setImageResource(R.drawable.ic_image_no_cover)
      return
    }

    val dimens = getDimens()
    Picasso
      .get()
      .load(file)
      .noFade()
      .error(R.drawable.ic_image_no_cover)
      .config(Bitmap.Config.RGB_565)
      .resize(dimens, dimens)
      .centerCrop()
      .into(albumCover)
  }

  override fun updateShuffleState(
    @ShuffleState shuffleState: String,
  ) {
    val shuffle = ShuffleChange.OFF != shuffleState
    val autoDj = ShuffleChange.AUTODJ == shuffleState

    val color = ContextCompat.getColor(this, if (shuffle) R.color.accent else R.color.button_dark)
    shuffleButton.setColorFilter(color)

    shuffleButton.setImageResource(if (autoDj) R.drawable.ic_headset_black_24dp else R.drawable.ic_shuffle_black_24dp)
  }

  override fun updateRepeat(
    @Mode mode: String,
  ) {
    @ColorRes var colorId = R.color.accent

    @DrawableRes var resId = R.drawable.ic_repeat_black_24dp

    //noinspection StatementWithEmptyBody
    when {
      Repeat.ALL.equals(mode, ignoreCase = true) -> {
        // Do nothing already set above
      }
      Repeat.ONE.equals(mode, ignoreCase = true) -> {
        resId = R.drawable.ic_repeat_one_black_24dp
      }
      else -> {
        colorId = R.color.button_dark
      }
    }

    val color = ContextCompat.getColor(this, colorId)
    repeatButton.setImageResource(resId)
    repeatButton.setColorFilter(color)
  }

  override fun updateVolume(
    volume: Int,
    mute: Boolean,
  ) {
    volumeBar.progress = volume
    val color = ContextCompat.getColor(this, R.color.button_dark)
    muteButton.setColorFilter(color)
    muteButton.setImageResource(if (mute) R.drawable.ic_volume_off_black_24dp else R.drawable.ic_volume_up_black_24dp)
  }

  override fun updatePlayState(
    @State state: String,
  ) {
    val accentColor = ContextCompat.getColor(this, R.color.accent)
    val tag = tag(state)

    if (playPauseButton.tag == tag) {
      return
    }
    @DrawableRes val resId: Int =
      when (state) {
        PlayerState.PLAYING -> {
          // Start the animation if the track is playing
          presenter.requestNowPlayingPosition()
          trackProgressAnimation(progressBar.progress, progressBar.max)
          R.drawable.ic_pause_circle_filled_black_24dp
        }
        PlayerState.PAUSED -> {
          // Stop the animation if the track is paused
          progressHelper.stop()
          R.drawable.ic_play_circle_filled_black_24dp
        }
        PlayerState.STOPPED -> {
          // Stop the animation if the track is paused
          progressHelper.stop()
          activateStoppedState()
          R.drawable.ic_play_circle_filled_black_24dp
        }
        else -> {
          R.drawable.ic_play_circle_filled_black_24dp
        }
      }

    playPauseButton.setColorFilter(accentColor)
    playPauseButton.setImageResource(resId)
    playPauseButton.tag = tag
  }

  /**
   * Starts the progress animation when called. If It was previously running then it restarts it.
   */
  private fun trackProgressAnimation(
    current: Int,
    total: Int,
  ) {
    progressHelper.stop()

    val tag = playPauseButton.tag
    if (STOPPED == tag || PAUSED == tag) {
      return
    }

    progressHelper.update(current, total)
  }

  private fun activateStoppedState() {
    progressBar.progress = 0
    trackProgressCurrent.text = getString(R.string.playback_progress, 0, 0)
  }

  override fun updateTrackInfo(info: TrackInfo) {
    artistLabel.text = info.artist
    titleLabel.text = info.title
    albumLabel.text =
      if (TextUtils.isEmpty(info.year)) {
        info.album
      } else {
        String.format("%s [%s]", info.album, info.year)
      }

    if (mShareActionProvider != null) {
      mShareActionProvider!!.setShareIntent(shareIntent)
    }
  }

  override fun updateConnection(status: Int) {
    if (status == Connection.OFF) {
      progressHelper.stop()
      activateStoppedState()
    }
  }

  /**
   * Responsible for updating the displays and seekbar responsible for the display of the track
   * duration and the
   * current progress of playback
   */

  override fun updateProgress(duration: UpdateDuration) {
    updateProgress(duration.position, duration.duration)
  }

  private fun updateProgress(
    current: Int,
    total: Int,
  ) {
    var currentSeconds = current / 1000
    var totalSeconds = total / 1000

    val currentMinutes = currentSeconds / 60
    val totalMinutes = totalSeconds / 60

    currentSeconds %= 60
    totalSeconds %= 60
    val finalTotalSeconds = totalSeconds
    val finalCurrentSeconds = currentSeconds

    trackDuration.text = getString(R.string.playback_progress, totalMinutes, finalTotalSeconds)
    trackProgressCurrent.text =
      getString(
        R.string.playback_progress,
        currentMinutes,
        finalCurrentSeconds,
      )

    progressBar.max = total
    progressBar.progress = current

    trackProgressAnimation(current, total)
  }

  override fun updateDuration(
    position: Int,
    duration: Int,
  ) {
    updateProgress(position, duration)
  }

  override fun updateScrobbleStatus(active: Boolean) {
    if (menu == null) {
      return
    }
    val scrobbleMenuItem = menu!!.findItem(R.id.menu_lastfm_scrobble) ?: return

    scrobbleMenuItem.isChecked = active
  }

  override fun updateLfmStatus(status: LfmStatus) {
    if (menu == null) {
      return
    }
    val favoriteMenuItem = menu!!.findItem(R.id.menu_lastfm_love) ?: return

    when (status) {
      LfmStatus.LOVED -> favoriteMenuItem.setIcon(R.drawable.ic_favorite_black_24dp)
      else -> favoriteMenuItem.setIcon(R.drawable.ic_favorite_border_black_24dp)
    }
  }

  override fun active(): Int = R.id.nav_home

  override fun progress(
    position: Int,
    duration: Int,
  ) {
    val currentProgress = progressBar.progress / 1000
    val currentMinutes = currentProgress / 60
    val currentSeconds = currentProgress % 60

    progressBar.progress = progressBar.progress + 1000
    trackProgressCurrent.text =
      getString(
        R.string.playback_progress,
        currentMinutes,
        currentSeconds,
      )
  }

  override fun onDestroy() {
    outOfDateDialog?.dismiss()
    changeLogDialog?.dismiss()
    super.onDestroy()
  }

  companion object {
    private const val PAUSED = "Paused"
    private const val STOPPED = "Stopped"
    private const val PLAYING = "Playing"

    fun tag(
      @State state: String,
    ): String =
      when (state) {
        PlayerState.PLAYING -> PLAYING
        PlayerState.PAUSED -> PAUSED
        else -> STOPPED
      }
  }
}
